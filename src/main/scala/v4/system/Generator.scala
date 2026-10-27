package boom.v4.system

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage
import difftest.DifftestModule
import freechips.rocketchip.diplomacy.DisableMonitors
import freechips.rocketchip.subsystem.WithExtMemSize
import org.chipsalliance.cde.config.{Config, Parameters}

object Generator {
  def main(args: Array[String]): Unit = {
    val top = args.sliding(2, 1).collectFirst { case Array("--top", x) => x }.getOrElse("SimTop")
    val config = args.sliding(2, 1).collectFirst { case Array("--config", x) => x }.getOrElse("MediumBoomConfig")
    require(!args.lastOption.contains("--mem-size"), "--mem-size requires a value in bytes")
    val memorySize = args.sliding(2, 1).collectFirst { case Array("--mem-size", x) => x }
      .map(x => if (x.startsWith("0x")) BigInt(x.drop(2), 16) else BigInt(x))
      .getOrElse(BigInt("80000000", 16))
    require(memorySize > 0, "--mem-size must be a positive size in bytes")
    val base: Config = config match {
      case "SmallBoomConfig" => new SmallBoomConfig
      case "MediumBoomConfig" => new MediumBoomConfig
      case "LargeBoomConfig" => new LargeBoomConfig
      case "SmallBoomFpgaConfig" => new SmallBoomFpgaConfig
      case "MediumBoomFpgaConfig" => new MediumBoomFpgaConfig
      case "LargeBoomFpgaConfig" => new LargeBoomFpgaConfig
      case other => throw new IllegalArgumentException(s"unknown BOOM config: $other")
    }
    val c = new Config(new WithExtMemSize(memorySize) ++ base)
    require(top == "SimTop", s"unknown BOOM top: $top")
    val gen = () => DisableMonitors(p =>
      DifftestModule.top(new BoomSimHarness()(p)))(c)
    def stageOptions(rest: List[String]): List[String] = rest match {
      case flag :: _ :: tail if Set("--top", "--config", "--mem-size")(flag) => stageOptions(tail)
      case head :: tail => head :: stageOptions(tail)
      case Nil => Nil
    }
    val stageArgs = stageOptions(args.toList).toArray
    val (chiselArgs, firtoolOptions) = DifftestModule.parseArgs(stageArgs)
    (new ChiselStage).execute(
      chiselArgs,
      Seq(ChiselGeneratorAnnotation(gen)) ++ firtoolOptions)
  }
}
