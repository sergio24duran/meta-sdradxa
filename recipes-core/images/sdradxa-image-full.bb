SUMMARY = "Full-featured image for Radxa Dragon boards with Qualcomm SoCs"
DESCRIPTION = "Development and integration image built on top of the minimal \
bring-up image. Adds wired Ethernet connectivity, GPIO userspace tools and \
every new feature as it is brought up."

require recipes-core/images/sdradxa-image-minimal.bb

# libgpiod is listed here instead of in the packagegroup because it is
# dynamically renamed to libgpiod3 at packaging time, which causes an
# allarch packagegroup QA error. Image recipes are not allarch.
IMAGE_INSTALL:append = " \
    packagegroup-sdradxa-connectivity \
    packagegroup-sdradxa-ai \
    packagegroup-sdradxa-camera \
    packagegroup-sdradxa-pytools \
    mavlink-router \
    mavsdk \
    libgpiod \
    libgpiod-tools \
    bpk-field-capture \
    bpk-storage \
    sw-bpk \
"
