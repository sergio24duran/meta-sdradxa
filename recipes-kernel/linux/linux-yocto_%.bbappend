# Extend linux-yocto COMPATIBLE_MACHINE to include sdradxa machines.
# meta-qcom restricts it to "qcom-armv8a|qcom-armv7a"; we add ours here.
# This bbappend has higher priority (sdradxa=6) than meta-qcom (5).
#
# qcm6490-idp.dtb is added by meta-qcom as a linux-yocto patch, so
# linux-yocto is the correct kernel for sdradxa-dragon-q6a.
COMPATIBLE_MACHINE:qcom = "qcom-armv8a|qcom-armv7a|sdradxa-dragon-q6a"
