package org.jenkinsci.plugins.android.connector.AndroidDevice

def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)

l.layout {
    def title = "App deployed"
    l.header(title:title)
    include(my,"sidepanel")
    l.main_panel {
        h1 {
            img(src:"${resURL}/plugin/android-device-connector/icons/48x48/android.png",alt:"[!]",height:48,width:48)
            text " ${title} (${my.displayName})"
        }

        pre msg
    }
}