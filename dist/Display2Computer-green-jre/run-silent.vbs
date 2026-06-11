Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
jar = root & "\display2computer.jar"
shell.CurrentDirectory = root
shell.Run "javaw -jar " & Chr(34) & jar & Chr(34), 0, False
