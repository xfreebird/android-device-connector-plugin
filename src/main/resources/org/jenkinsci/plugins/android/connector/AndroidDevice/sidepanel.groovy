package org.jenkinsci.plugins.android.connector.AndroidDevice
import org.jenkinsci.plugins.android.connector.AndroidDeviceList

l=namespace(lib.LayoutTagLib)

l.header()
l.side_panel {
    l.tasks {
        l.task(icon:"images/24x24/up.png", href:'..', title:_("Back to Device List"))
        l.task(icon:"images/24x24/setting.png", href:"deploy", title:_("Deploy App"),
                permission: AndroidDeviceList.DEPLOY, it:app)
    }
}
