package boom.v4.system

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.devices.debug.DebugModuleKey
import freechips.rocketchip.rocket.ASIdBits
import freechips.rocketchip.subsystem._
import boom.v4.common._

class WithExternalResetVector extends Config((site, here, up) => {
  case HasTilesExternalResetVectorKey => true
})

/** DiffTest drives execution directly and does not need a DMI/JTAG model. */
class WithNoDebugModule extends Config((site, here, up) => {
  case DebugModuleKey => None
})

class WithBoomSupervisor extends Config((site, here, up) => {
  case ASIdBits => 16
  case TilesLocated(InSubsystem) => up(TilesLocated(InSubsystem), site).map {
    case tp: BoomTileAttachParams => tp.copy(tileParams = tp.tileParams.copy(
      core = tp.tileParams.core.copy(
        useSupervisor = true,
        useVM = true,
        useDebug = false,
        nPMPs = 16)))
    case other => other
  }
})

class BoomBaseConfig(
    mmioBase: BigInt = BigInt("10000000", 16),
    mmioSize: BigInt = BigInt("100", 16),
    mmioDataWidth: Int = 128,
    mmioIdBits: Int = 4,
    mmioMaxXferBytes: Int = 16,
    memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithNoDebugModule ++
  new WithExternalResetVector ++
  new WithCoherentBusTopology ++
  new WithExtMemSize(memSize) ++
  new WithDefaultMemPort ++
  new WithCustomMMIOPort(
    base_addr = mmioBase,
    base_size = mmioSize,
    // The Large BOOM coherent bus presents a 128-bit external AXI beat;
    // choosing that width here keeps the MMIO node valid for every config.
    data_width = mmioDataWidth,
    id_bits = mmioIdBits,
    maxXferBytes = mmioMaxXferBytes) ++
  new WithDefaultSlavePort ++
  new WithTimebase(BigInt(1000000)) ++
  new WithDTS("freechips,rocketchip-unknown", Nil) ++
  new BaseSubsystemConfig)

class SmallBoomConfig(memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithBoomSupervisor ++ new WithNSmallBooms(1) ++ new BoomBaseConfig(memSize = memSize))
class MediumBoomConfig(memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithBoomSupervisor ++ new WithNMediumBooms(1) ++ new BoomBaseConfig(memSize = memSize))
class LargeBoomConfig(memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithBoomSupervisor ++ new WithNLargeBooms(1) ++ new BoomBaseConfig(memSize = memSize))

/** FPGA image: MMIO goes to the board AXI UART16550 at 0x310b0000. */
class BoomFpgaBaseConfig(
    memSize: BigInt = BigInt("80000000", 16),
    mmioMaxXferBytes: Int = 8) extends Config(
  new BoomBaseConfig(
    memSize = memSize,
    mmioBase = BigInt("310b0000", 16),
    mmioSize = BigInt("10000", 16),
    mmioDataWidth = 64,
    mmioIdBits = 2,
    mmioMaxXferBytes = mmioMaxXferBytes))

class SmallBoomFpgaConfig(memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithBoomSupervisor ++ new WithNSmallBooms(1) ++ new BoomFpgaBaseConfig(memSize))
class MediumBoomFpgaConfig(memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithBoomSupervisor ++ new WithNMediumBooms(1) ++ new BoomFpgaBaseConfig(memSize))
class LargeBoomFpgaConfig(memSize: BigInt = BigInt("80000000", 16)) extends Config(
  new WithBoomSupervisor ++ new WithNLargeBooms(1) ++
  // The Large instruction TLB requires 16-byte transfers, over two AXI beats.
  new BoomFpgaBaseConfig(memSize, mmioMaxXferBytes = 16))
