SBT		?= sbt
#SBT_FLAGS	?= -Dsbt.log.noformat=true

.PHONY: init checkstyle verilog emu clean

init:
	git submodule update --init --recursive rocket-chip difftest

checkstyle:
	$(SBT) $(SBT_FLAGS) scalastyle test:scalastyle

CONFIG ?= MediumBoomConfig
MEM_SIZE ?= 0x80000000
DIFFTEST_CONFIG ?=
TOP ?= SimTop
NOOP_HOME ?= $(CURDIR)
BUILD_DIR = $(abspath $(NOOP_HOME))/build
RTL_DIR := $(abspath $(BUILD_DIR))/rtl
MILL ?= mill
MILL_VERSION ?= 0.11.1

verilog:
	mkdir -p $(RTL_DIR)
	NOOP_HOME=$(abspath $(NOOP_HOME)) MILL_VERSION=$(MILL_VERSION) \
		$(MILL) -i boom.runMain boom.v4.system.Generator \
		--top $(TOP) --config $(CONFIG) --mem-size $(MEM_SIZE) --target-dir $(RTL_DIR) \
		--target systemverilog --split-verilog \
		$(if $(strip $(DIFFTEST_CONFIG)),--difftest-config $(DIFFTEST_CONFIG),)
	test -s $(RTL_DIR)/$(TOP).sv
	test -s $(BUILD_DIR)/generated-src/difftest_profile.json

emu: verilog
	$(MAKE) -C difftest emu NOOP_HOME=$(abspath $(NOOP_HOME)) SIM_TOP=$(TOP) NUM_CORES=1 \
		WITH_CHISELDB=0 WITH_CONSTANTIN=0

clean:
	@build_dir=$$(realpath -m -- "$(BUILD_DIR)") || exit $$?; \
	repo_dir=$$(pwd -P); \
	case "$$repo_dir/" in \
		"$$build_dir/"*) echo "refusing to remove source directory: $$build_dir" >&2; exit 2 ;; \
	esac; \
	case "$$build_dir" in \
		"$$repo_dir"/build|"$$repo_dir"/build/*|/tmp/boom-*) rm -rf -- "$$build_dir" ;; \
		*) echo "refusing to remove BUILD_DIR: $(BUILD_DIR)" >&2; exit 2 ;; \
	esac
