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

# RIL
PRODUCT_PROPERTY_OVERRIDES += \
    ro.telephony.ril_class=SerranoLTEUSCRIL

# Stock RIL and GPS files
PRODUCT_COPY_FILES += \
    device/samsung/serranolteusc/proprietary/lib/libril.so:system/lib/libril.so \
    device/samsung/serranolteusc/proprietary/lib/libsec-ril.so:system/lib/libsec-ril.so \
    device/samsung/serranolteusc/proprietary/lib/libloc_api_v02.so:system/lib/libloc_api_v02.so \
    device/samsung/serranolteusc/proprietary/vendor/lib/liblbs_core.so:system/vendor/lib/liblbs_core.so 


# Inherit from serrano-common
$(call inherit-product, device/samsung/serrano-common/serrano-common.mk)
$(call inherit-product, device/samsung/serrano-common/nfc.mk)

# Device overlay
# Control all overlays here because we do not want the Mms xmls from qcom-common
DEVICE_PACKAGE_OVERLAYS := device/samsung/serranolteusc/overlay
DEVICE_PACKAGE_OVERLAYS += device/samsung/serrano-common/overlay
DEVICE_PACKAGE_OVERLAYS += device/samsung/qcom-common/overlay/frameworks
DEVICE_PACKAGE_OVERLAYS += device/samsung/qcom-common/overlay/packages/services
DEVICE_PACKAGE_OVERLAYS += device/samsung/qcom-common/overlay/packages/apps/Contacts
DEVICE_PACKAGE_OVERLAYS += device/samsung/msm8930-common/overlay

# Permissions
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.telephony.cdma.xml:system/etc/permissions/android.hardware.telephony.cdma.xml

# Ramdisk with TWRP
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/init.carrier.rc:root/init.carrier.rc 

### $(LOCAL_PATH)/twrp.fstab:root/etc/twrp.fstab

