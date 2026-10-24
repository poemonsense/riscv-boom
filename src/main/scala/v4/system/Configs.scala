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

class BoomBaseConfig extends Config(
  new WithNoDebugModule ++
  new WithExternalResetVector ++
  new WithCoherentBusTopology ++
  new WithDefaultMemPort ++
  new WithCustomMMIOPort(
    base_addr = BigInt("10000000", 16),
    base_size = BigInt("100", 16),
    // The Large BOOM coherent bus presents a 128-bit external AXI beat;
    // choosing that width here keeps the MMIO node valid for every config.
    data_width = 128,
    id_bits = 4,
    maxXferBytes = 16) ++
  new WithDefaultSlavePort ++
  new WithTimebase(BigInt(1000000)) ++
  new WithDTS("freechips,rocketchip-unknown", Nil) ++
  new BaseSubsystemConfig)

class SmallBoomConfig extends Config(new WithBoomSupervisor ++ new WithNSmallBooms(1) ++ new BoomBaseConfig)
class MediumBoomConfig extends Config(new WithBoomSupervisor ++ new WithNMediumBooms(1) ++ new BoomBaseConfig)
class LargeBoomConfig extends Config(new WithBoomSupervisor ++ new WithNLargeBooms(1) ++ new BoomBaseConfig)
class MediumBoomMem2GConfig extends Config(
  new WithExtMemSize(BigInt("80000000", 16)) ++
  new WithBoomSupervisor ++
  new WithNMediumBooms(1) ++
  new BoomBaseConfig)
