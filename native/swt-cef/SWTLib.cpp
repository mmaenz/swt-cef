#include "SWTApp.h"
#include "ClientHandler.h"
#include <jni.h>

#include <direct.h>
#include <windows.h>

char szWorkingDir[MAX_PATH];  // The current working directory
bool message_loop = false;
HWND mainBrowserHandle = NULL;

extern "C"
JNIEXPORT int Java_org_eclipse_swt_cef_Chromium_init(JNIEnv *env, jclass cls, jlong handle) {
    if (_getcwd(szWorkingDir, MAX_PATH) == NULL) {
        szWorkingDir[0] = 0;
    }
    CefString workingPath = CefString(szWorkingDir);

    CefMainArgs main_args(GetModuleHandle(NULL));
    CefSettings settings;

    CefRefPtr<SWTApp> app(new SWTApp((long)((void*)handle)));

#ifdef _DEBUG
    CefString debugPath = CefString("D:\\12_Dev\\Git\\swt-cef\\java\\target\\classes\\os\\win32\\x86_64");
    CefString(&settings.browser_subprocess_path) = debugPath.ToString() + "\\subProcess.exe";
#else
    CefString(&settings.browser_subprocess_path) = workingPath.ToString() + "\\subProcess.exe";
#endif

    settings.multi_threaded_message_loop = message_loop;
    settings.log_severity = LOGSEVERITY_DISABLE;
    settings.no_sandbox = true;

    CefInitialize(main_args, settings, app, NULL);
}

extern "C"
JNIEXPORT jlong Java_org_eclipse_swt_cef_Chromium_create(JNIEnv * env, jclass cls, jlong parentId, jstring url) {
    // Register the window class.
    HWND hMain = (HWND)((void*)parentId);

    const char* chr = (char*)env->GetStringUTFChars(url, 0);
    CefString pageURL = CefString(chr);
    env->ReleaseStringUTFChars(url, chr);

    CefWindowInfo        info;
    CefBrowserSettings   b_settings;
    CefRefPtr<CefClient> client(new ClientHandler);

    RECT rect;
    GetClientRect(hMain, &rect);
    info.SetAsChild(hMain, rect);
    //info.SetAsPopup(hMain, CefString("Window"));

    CefBrowserHost::CreateBrowser(info, client.get(), pageURL, b_settings, NULL, NULL);
    return ((jlong)(void*)client);
}
extern "C"
JNIEXPORT int Java_org_eclipse_swt_cef_Chromium_work()
{
	CefRunMessageLoop();
	return 0;
}

extern "C"
JNIEXPORT int Java_org_eclipse_swt_cef_Chromium_release()
{
    CefShutdown();
    return 0;
}
