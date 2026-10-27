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
BUILD_DIR ?= $(abspath build/boom-$(CONFIG))
RTL_DIR := $(BUILD_DIR)/build/rtl
MILL ?= mill
MILL_VERSION ?= 0.11.1

verilog:
	mkdir -p $(RTL_DIR)
	NOOP_HOME=$(BUILD_DIR) MILL_VERSION=$(MILL_VERSION) \
		$(MILL) -i boom.runMain boom.v4.system.Generator \
		--top $(TOP) --config $(CONFIG) --mem-size $(MEM_SIZE) --target-dir $(RTL_DIR) \
		--target systemverilog --split-verilog \
		$(if $(strip $(DIFFTEST_CONFIG)),--difftest-config $(DIFFTEST_CONFIG),)
	test -s $(RTL_DIR)/$(TOP).sv
	test -s $(BUILD_DIR)/build/generated-src/difftest_profile.json

emu: verilog
	$(MAKE) -C difftest emu NOOP_HOME=$(BUILD_DIR) \
		DESIGN_DIR=$(BUILD_DIR) BUILD_DIR=$(BUILD_DIR)/build SIM_TOP=$(TOP) NUM_CORES=1 \
		WITH_CHISELDB=0 WITH_CONSTANTIN=0

clean:
	@case "$(BUILD_DIR)" in \
		"$(CURDIR)"/build/*|/tmp/boom-*) rm -rf -- "$(BUILD_DIR)" ;; \
		*) echo "refusing to remove BUILD_DIR: $(BUILD_DIR)" >&2; exit 2 ;; \
	esac
