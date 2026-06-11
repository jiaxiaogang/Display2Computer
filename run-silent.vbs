Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
javaw = "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9\bin\javaw.exe"
jar = root & "\target\display2computer.jar"
shell.CurrentDirectory = root
shell.Run Chr(34) & javaw & Chr(34) & " -jar " & Chr(34) & jar & Chr(34), 0, False
