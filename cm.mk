# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Enhanced NFC
$(call inherit-product, vendor/cm/config/nfc_enhanced.mk)

$(call inherit-product, device/samsung/serranolteusc/full_serranolteusc.mk)

PRODUCT_BUILD_PROP_OVERRIDES += PRODUCT_NAME=serranolteusc TARGET_DEVICE=serranolteusc BUILD_FINGERPRINT="samsung/serranolteusc/serranolteusc:4.4.2/KOT49H/R890TYUBNE4:user/release-keys" PRIVATE_BUILD_DESC="serranolteusc-user 4.4.2 KOT49H R890TYUBNE4 release-keys"

PRODUCT_DEVICE := serranolteusc
PRODUCT_NAME := cm_serranolteusc
