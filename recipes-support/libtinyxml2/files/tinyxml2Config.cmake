# Minimal CMake package config for tinyxml2.
#
# meta-oe builds libtinyxml2 with meson, which ships tinyxml2.pc but no CMake
# package config. Consumers that use find_package(tinyxml2) in CONFIG mode
# (MAVSDK, libmavlike) need the tinyxml2::tinyxml2 imported target, which this
# provides. Installed by the libtinyxml2 bbappend into ${libdir}/cmake/tinyxml2.
get_filename_component(_tinyxml2_prefix "${CMAKE_CURRENT_LIST_DIR}/../../.." ABSOLUTE)

if(NOT TARGET tinyxml2::tinyxml2)
    add_library(tinyxml2::tinyxml2 SHARED IMPORTED)
    set_target_properties(tinyxml2::tinyxml2 PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES "${_tinyxml2_prefix}/include"
        IMPORTED_LOCATION "${_tinyxml2_prefix}/lib/libtinyxml2.so"
        IMPORTED_SONAME "libtinyxml2.so.10")
endif()

set(tinyxml2_FOUND TRUE)
unset(_tinyxml2_prefix)
