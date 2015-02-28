package org.jenkinsci.plugins.android.connector.AndroidDeviceList

def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)

l.layout {
    def title = _("Connected Android Devices");
    l.header(title:title)
    l.main_panel {
        h1 {
            img(src:"${resURL}/plugin/android-device-connector/icons/48x48/android.png",alt:"[!]",height:48,width:48)
            text " "
            text title
        }

        p _("blurb")

        table(id:"devices", class:"sortable pane bigtable") {
            tr {
                th(width:32) {
                    // column to show icon
                }
                th(initialSortDir:"down") {
                    text _("Device Name")
                }
                th {
                    text _("OS Version")
                }
                th {
                    text _("ID")
                }
                th {
                    text _("Connected to")
                }
            }
            my.devices.entries().each { e ->
                def dev = e.value;
                tr {
                    td {
                        a(href:dev.uniqueDeviceId) {
                            img(src:"${resURL}/plugin/android-device-connector/icons/24x24/android.png")
                        }
                    }
                    td {
                        a(href:dev.uniqueDeviceId, dev.deviceName)
                    }
                    td {
                        text dev.buildVersionRelease;
                    }
                    td {
                        text dev.uniqueDeviceId;
                    }
                    td {
                        a(href:"$rootURL/${e.key.url}",  e.key.name)
                    }
                }
            }
        }

        f.form(method:"POST",action:"refresh") {
            div (align:"right",style:"margin-top:1em") {
                f.submit(value:_("Refresh"))
            }
        }
    }
}