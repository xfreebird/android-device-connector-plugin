package org.jenkinsci.plugins.android.connector.AndroidDevice

def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)

l.layout {
    def title = "Deploy APK to ${my.deviceName}"
    l.header(title:title)
    include(my,"sidepanel")
    l.main_panel {
        h1 {
            img(src:"${resURL}/plugin/android-device-connector/icons/48x48/android.png",alt:"[!]",height:48,width:48)
            text " ${title} (${my.uniqueDeviceId})"
        }

        f.form(method:"POST",action:"doDeploy") {
            f.entry(title:"APK file to deploy") {
                input(name:"apk",type:"file")
            }
            f.block {
                    f.submit(value:"Deploy")
            }
        }
    }
}