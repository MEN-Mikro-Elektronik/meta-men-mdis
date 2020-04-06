SUMMARY = "MEN MDIS5 System Package for Linux"
HOMEPAGE = "http://www.men.de"

LICENSE = "GPLv2"
SECTION = "misc"

inherit module module-base

DEPENDS += "virtual/kernel pciutils rsync-native"

PR = "r0"

FILES_${PN}_append = " \
    ${sysconfdir} \
    ${libdir} \
    ${includedir} \
    ${bindir} \
"

CONFFILES_${PN} += "${sysconfdir}/mdis/cpu.bin \
    "

FILES_${PN}-dev = "${includedir}"

LIC_FILES_CHKSUM="\
    file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6 \
    file://${COMMON_LICENSE_DIR}/LGPL-2.1;md5=1a6d268fd218675ffea8be556788b780 \
    "

SRC_URI = " \
    gitsm://git@github.com/MEN-Mikro-Elektronik/13MD05-90.git;protocol=http;branch=jpe-dev;name=mdis5;destsuffix=git/mdis5 \
    file://0001-Remove_use_of_realpath.patch \
"

# Used MDIS version: This version is development branch jpe-dev
SRCREV_mdis5 = "e8e0ad22de4d35a1fdd8f0be7b163d0b9e099f15"

S = "${WORKDIR}"

# Temporary MDIS installation directory path
MDIS_YOCTO_DIR = "${WORKDIR}/mdis_install"

addtask do_patch before do_configure

do_configure() {
    cd ${WORKDIR}/git/mdis5
    bbnote "Run INSTALL.sh script"
    ./INSTALL.sh -p ${MDIS_YOCTO_DIR} --install-only
    cd ${WORKDIR}
    mkdir -p target
    echo "KERNEL_CC := ${CC}" >  target/.kernelsettings
    echo "KERNEL_LD := ${LD}" >> target/.kernelsettings
    echo "KERNEL_CFLAGS := ${CFLAGS} -Wno-implicit-function-declaration"  >> target/.kernelsettings
    echo "KERNEL_LFLAGS := ${LDFLAGS}"   >> target/.kernelsettings
    echo "KERNEL_ARCH := ARM" >> target/.kernelsettings
    sed -i 's/-Werror=format-security//g' ${WORKDIR}/target/.kernelsettings
    cp ${WORKDIR}/target/.kernelsettings ${MDIS_YOCTO_DIR}/BUILD/MDIS/DEVTOOLS/.kernelsettings
    if [ -f Makefile ]; then mv Makefile target/; fi
    if [ -f system.dsc ]; then mv system.dsc target/; fi
}

do_compile() {
    set OLDPWD ${PWD}
    cd ${WORKDIR}/target

    unset LDFLAGS
    make \
      V=1 \
      LIN_KERNEL_DIR=${STAGING_KERNEL_BUILDDIR} \
      MEN_LIN_DIR=${MDIS_YOCTO_DIR} \
      LIB_INSTALL_DIR=${D}${libdir} \
      STATIC_LIB_INSTALL_DIR=${D}${libdir} \
      BIN_INSTALL_DIR=${D}${bindir} \
      DESC_INSTALL_DIR=${D}${sysconfdir}/mdis \
      MODS_INSTALL_DIR=${D}/lib/modules/${KERNEL_VERSION}/misc \
      TARGET_TREE=${D} 

    cd ${OLDPWD}
    unset OLDPWD
}

do_package_prepend() {

}

do_install() {
    set OLDPWD ${PWD}
    cd ${WORKDIR}/target
    oe_runmake \
         LIN_KERNEL_DIR=${STAGING_KERNEL_BUILDDIR} \
         MEN_LIN_DIR=${MDIS_YOCTO_DIR} \
         LIB_INSTALL_DIR=${D}${libdir} \
         STATIC_LIB_INSTALL_DIR=${D}${libdir} \
         BIN_INSTALL_DIR=${D}${bindir} \
         DESC_INSTALL_DIR=${D}${sysconfdir}/mdis \
         MODS_INSTALL_DIR=${D}/lib/modules/${KERNEL_VERSION}/misc \
         TARGET_TREE=${D} install
  
    # TODO: headers are needed for other dependcies but should not be installed
    install -d ${D}${includedir}/MEN
    install -m 0660 ${MDIS_YOCTO_DIR}/INCLUDE/COM/MEN/men_typs.h         ${D}${includedir}/MEN
    install -m 0660 ${MDIS_YOCTO_DIR}/INCLUDE/COM/MEN/oss.h              ${D}${includedir}/MEN
    install -m 0660 ${MDIS_YOCTO_DIR}/INCLUDE/COM/MEN/chameleon.h        ${D}${includedir}/MEN
    install -m 0660 ${MDIS_YOCTO_DIR}/INCLUDE/NATIVE/MEN/men_chameleon.h ${D}${includedir}/MEN
    install -m 0660 ${MDIS_YOCTO_DIR}/INCLUDE/NATIVE/MEN/oss_os.h        ${D}${includedir}/MEN

    cd ${OLDPWD}
    unset OLDPWD
}

do_clean_preappend() {
    set OLDPWD ${PWD}
    cd ${S}/target
    oe_runmake 
        LIN_KERNEL_DIR=${STAGING_KERNEL_BUILDDIR} \
        MEN_LIN_DIR=${WORKDIR} \
        LIB_INSTALL_DIR=${D}${libdir} \
        STATIC_LIB_INSTALL_DIR=${D}${libdir} \
        BIN_INSTALL_DIR=${D}${bindir} \
        DESC_INSTALL_DIR=${D}${sysconfdir}/mdis \
        MODS_INSTALL_DIR=${D}/lib/modules/${KERNEL_VERSION}/misc clean

    cd ${OLDPWD}
    unset OLDPWD
}

INSANE_SKIP_${PN} += "installed-vs-shipped"
INSANE_SKIP_${PN} = "ldflags"
INSANE_SKIP_${PN}-dev = "ldflags"
INSANE_SKIP_${PN}-dev += "dev-elf"
