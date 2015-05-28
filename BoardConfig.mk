# Copyright (C) 2013 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This file sets variables that control the way modules are built
# thorughout the system. It should not be used to conditionally
# disable makefiles (the proper mechanism to control what gets
# included in a build is to use PRODUCT_PACKAGES in a product
# definition file).
#

# Inherit from common serrano
-include device/samsung/serrano-common/BoardConfigCommon.mk

# Assert
TARGET_OTA_ASSERT_DEVICE := serranolteusc,R890,SCH-R890

# Kernel
TARGET_KERNEL_VARIANT_CONFIG := msm8930_serrano_usc_defconfig

# NFC
BOARD_HAVE_NFC := true

# LED
BOARD_HAVE_MULTI_COLOR_LED := true

# RECOVERY TWRP ---------------------------------------------------------------------------------
RECOVERY_VARIANT := twrp
TARGET_RECOVERY_FSTAB := device/samsung/serranolteusc/twrp.fstab
TARGET_RECOVERY_INITRC := device/samsung/serranolteusc/rootdir/init.recovery.rc
DEVICE_RESOLUTION := 540x960
RECOVERY_GRAPHICS_USE_LINELENGTH := true
TARGET_USE_CUSTOM_LUN_FILE_PATH := /sys/devices/platform/msm_hsusb/gadget/lun%d/file
BOARD_KERNEL_CMDLINE := androidboot.hardware=qcom user_debug=23 androidboot.bootdevice=msm_sdcc.1 androidboot.selinux=permissive
# -----------------------------------------------------------------------------------------------

# Custom RIL class
BOARD_RIL_CLASS := ../../../device/samsung/serranolteusc/ril/
