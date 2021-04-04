cmake_minimum_required (VERSION 3.10)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

project (swt-cef)

find_package(Java REQUIRED)
# pkg_check_modules(GTK3 REQUIRED gtk+-3.0)

include(ExternalProject)

set(CEF_DIR ${CMAKE_CURRENT_BINARY_DIR}/bin)
set(CEF_PREFIX ${CMAKE_CURRENT_BINARY_DIR}/cef)
set(JAVA_RES_DIR "${CMAKE_SOURCE_DIR}/../../java/src/main/resources/os/win32/x86_64")

ExternalProject_Add(cef-minimal
	PREFIX ${CEF_PREFIX}
    #URL https://cef-builds.spotifycdn.com/cef_binary_89.0.17+ge7bbb1d+chromium-89.0.4389.114_windows64_minimal.tar.bz2
	URL file://D:/12_Dev/Git/swt-cef/dependencies/cef_binary_89.0.17+ge7bbb1d+chromium-89.0.4389.114_windows64_minimal.zip
	#URL https://cef-builds.spotifycdn.com/cef_binary_73.1.13+g6e3c989+chromium-73.0.3683.75_windows64_minimal.tar.bz2
	INSTALL_COMMAND cmake -E copy_directory ${CEF_PREFIX}/src/cef-minimal/include ${CEF_DIR}/include
        COMMAND cmake -E copy_directory ${CEF_PREFIX}/src/cef-minimal/Release ${CEF_DIR}/lib
        COMMAND cmake -E copy_directory ${CEF_PREFIX}/src/cef-minimal/Resources ${CEF_DIR}/lib
        COMMAND cmake -E copy ${CEF_PREFIX}/src/cef-minimal-build/libcef_dll_wrapper/$<$<CONFIG:Debug>:Debug>$<$<CONFIG:Release>:Release>/libcef_dll_wrapper.lib ${CEF_DIR}/lib
)

set(JDK_PREFIX ${CMAKE_CURRENT_BINARY_DIR}/jdk)

ExternalProject_Add(jdk
	PREFIX ${JDK_PREFIX}
    #URL https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk11u-2021-03-25-04-28/OpenJDK11U-jdk_x64_windows_hotspot_2021-03-25-04-28.zip 
    #https://cef-builds.spotifycdn.com/cef_binary_73.1.13+g6e3c989+chromium-73.0.3683.75_windows64_minimal.tar.bz2
    URL file://D:/12_Dev/Git/swt-cef/dependencies/OpenJDK11U-jdk_x64_windows_hotspot_2021-03-25-04-28.zip
	CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND cmake -E copy_directory ${JDK_PREFIX}/src/jdk ${CEF_DIR}/jdk
    	COMMAND cmake -E copy_directory ${CEF_DIR}/lib ${CEF_DIR}/jdk/jre/bin
)

link_directories(
	${CEF_DIR}/lib
)

if(MSVC)
	add_compile_options(
		$<$<CONFIG:>:/MT> #---------|
		$<$<CONFIG:Debug>:/MTd> #---|-- Statically link the runtime libraries
		$<$<CONFIG:Release>:/MT> #--|
	)
endif()

#add_dependencies(jdk cef-minimal)

add_executable(subProcess
	subProcess.cpp
)

add_dependencies(subProcess cef-minimal jdk)

target_include_directories(subProcess PUBLIC
	${CEF_DIR}
)

target_link_libraries(subProcess
	libcef_dll_wrapper
	libcef
)

install(TARGETS subProcess
	RUNTIME DESTINATION "${CEF_DIR}/lib")

add_library(swt-cef SHARED
	ClientHandler.cpp
	SWTApp.cpp
	SWTLib.cpp
)

add_dependencies(swt-cef cef-minimal jdk)

target_include_directories(swt-cef PUBLIC
	${CEF_DIR}
	${CEF_DIR}/jdk/include
	${CEF_DIR}/jdk/include/win32
)

target_link_libraries(swt-cef
	libcef_dll_wrapper
	libcef
)


install(TARGETS swt-cef
	RUNTIME DESTINATION "${CEF_DIR}/lib")
	
add_executable(cef-test
	testApp.cpp
)

install(TARGETS cef-test
	DESTINATION "${CEF_DIR}/lib")
	
add_dependencies(cef-test cef-minimal jdk)

target_include_directories(cef-test PUBLIC
	${CEF_DIR}
)

target_link_libraries(cef-test
	libcef_dll_wrapper
	libcef
)

MESSAGE(${JAVA_RES_DIR})

install(TARGETS swt-cef subProcess cef-test
		RUNTIME DESTINATION ${JAVA_RES_DIR}
        LIBRARY DESTINATION ${JAVA_RES_DIR}
)
install(DIRECTORY ${CEF_DIR}/lib/ DESTINATION ${JAVA_RES_DIR})