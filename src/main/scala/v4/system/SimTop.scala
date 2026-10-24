package boom.v4.system

import chisel3._
import chisel3.util._
import difftest.{DifftestMemIO, DifftestModule, DifftestTopIO, HasDiffTestInterfaces, UARTIO}
import difftest.common.{AXI4Bundle => DifftestAXI4Bundle}
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem._
import org.chipsalliance.cde.config.Parameters

/** Minimal emulator harness.  AXI RAMs are supplied by the DiffTest runtime. */
class BoomSimHarness(implicit p: Parameters)
    extends Module with HasDiffTestInterfaces {
  val ldut = LazyModule(new BoomRocketSystem)
  val dut = Module(ldut.module)
  dut.dontTouchPorts()
  ldut.io_clocks.get.elements.values.foreach(_.clock := clock)
  ldut.io_clocks.get.elements.values.foreach(_.reset := reset)
  dut.reset_vector.foreach(_ := "h80000000".U)
  if (!DifftestModule.isFPGA) {
    SimDifftestAXIMem.connectMem(ldut)
  }
  private val fpgaMem = Option.when(DifftestModule.isFPGA) {
    val mem = IO(chiselTypeOf(ldut.mem_axi4.head)).suggestName("mem")
    DifftestAXI4Bundle.connectRecord(mem, ldut.mem_axi4.head)
    mem
  }
  val uart = SimAXIUart.connectMMIO(ldut).head
  val difftestUart = IO(new UARTIO)
  difftestUart.out.valid := uart.valid
  difftestUart.out.ch := uart.bits
  difftestUart.in.valid := false.B
  val debugSuccess = WireInit(false.B)
  Debug.connectDebug(ldut.debug, ldut.resetctrl, ldut.psd, clock, reset.asBool, debugSuccess)

  override def cpuName: Option[String] = Some("boom")
  override def difftestMemIO: Option[DifftestMemIO] =
    fpgaMem.map(DifftestMemIO(_))
  override def connectTopIOs(difftest: DifftestTopIO): Unit = {
    difftest.uart <> difftestUart
  }
}
