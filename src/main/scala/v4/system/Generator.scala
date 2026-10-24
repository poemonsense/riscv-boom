package boom.v4.system

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage
import difftest.DifftestModule
import freechips.rocketchip.diplomacy.DisableMonitors
import org.chipsalliance.cde.config.{Config, Parameters}

object Generator {
  def main(args: Array[String]): Unit = {
    val top = args.sliding(2, 1).collectFirst { case Array("--top", x) => x }.getOrElse("SimTop")
    val config = args.sliding(2, 1).collectFirst { case Array("--config", x) => x }.getOrElse("MediumBoomConfig")
    val c: Config = config match {
      case "SmallBoomConfig" => new SmallBoomConfig
      case "MediumBoomConfig" => new MediumBoomConfig
      case "LargeBoomConfig" => new LargeBoomConfig
      case "MediumBoomMem2GConfig" => new MediumBoomMem2GConfig
      case other => throw new IllegalArgumentException(s"unknown BOOM config: $other")
    }
    require(top == "SimTop", s"unknown BOOM top: $top")
    val gen = () => DisableMonitors(p =>
      DifftestModule.top(new BoomSimHarness()(p)))(c)
    val stageArgs = args.filterNot(a => a == "--top" || a == top || a == "--config" || a == config)
    val (chiselArgs, firtoolOptions) = DifftestModule.parseArgs(stageArgs)
    (new ChiselStage).execute(
      chiselArgs,
      Seq(ChiselGeneratorAnnotation(gen)) ++ firtoolOptions)
  }
}
