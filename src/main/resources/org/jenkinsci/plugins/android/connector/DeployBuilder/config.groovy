package org.jenkinsci.plugins.android.connector.DeployBuilder

def f = namespace(lib.FormTagLib)

f.entry(title:"Path to .apk file(s)", field:"path") {
    f.textbox()
}
f.entry(title:"Device", field:"udid") {
    f.textbox()
}
