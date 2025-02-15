#ifndef _SWTApp_H_
#define _SWTApp_H_

#include "ClientHandler.h"
#include <include/cef_app.h>

class SWTApp: public CefApp, public CefBrowserProcessHandler {
public:
	SWTApp(long parentId);
	virtual ~SWTApp();

	virtual CefRefPtr<CefBrowserProcessHandler> GetBrowserProcessHandler() override {
		return this;
	}

	virtual void OnBeforeCommandLineProcessing(
			const CefString& process_type,
			CefRefPtr<CefCommandLine> command_line);

	virtual void OnContextInitialized() override;
private:
	long parentId;
	IMPLEMENT_REFCOUNTING(SWTApp);
};

#endif // _SWTApp_H_
