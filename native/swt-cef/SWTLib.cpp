#include "SWTApp.h"
#include "ClientHandler.h"
#include <jni.h>

#include <windows.h>

extern "C"
JNIEXPORT int Java_org_eclipse_swt_cef_Chromium_start(JNIEnv *env, jclass cls, jlong parentId)
{
    long long handle = (long long)parentId;
    CefMainArgs main_args;

    CefRefPtr<SWTApp> app(new SWTApp(handle));

    // Execute the secondary process, if any.
    int exit_code = CefExecuteProcess(main_args, app.get(), NULL);

    if (exit_code >= 0) {
        exit(exit_code);
    }

    // Register the window class.
    HWND hwnd = (HWND)handle;

    RECT rect;
    GetClientRect(hwnd, &rect);

    CefSettings settings;
    CefInitialize(main_args, settings, app.get(), NULL);
    CefWindowInfo        info;
    CefBrowserSettings   b_settings;
    CefRefPtr<CefClient> client(new ClientHandler);
    std::string path = "https://google.de";
    CefRefPtr<CefCommandLine> command_line = CefCommandLine::GetGlobalCommandLine();

    if (command_line->HasSwitch("url")) {
        path = command_line->GetSwitchValue("url");
    }

    info.SetAsChild(hwnd, rect);
    CefBrowserHost::CreateBrowser(info, client.get(), path, b_settings, NULL, NULL);
    int result = 0;	return 0;
}

extern "C"
JNIEXPORT int Java_org_eclipse_swt_cef_Chromium_work()
{
	CefDoMessageLoopWork();
	return 0;
}

extern "C"
JNIEXPORT int Java_org_eclipse_swt_cef_Chromium_release()
{
    CefShutdown();
    return 0;
}
