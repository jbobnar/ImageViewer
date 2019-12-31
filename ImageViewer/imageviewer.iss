[Setup]
AppName=Image Viewer
AppVersion=3.0.0
DefaultDirName={pf}\ImageViewer
DefaultGroupName=Image Viewer
Compression=lzma2
SolidCompression=yes
OutputDir=F:\
OutputBaseFilename=ImageViewer.Setup

[Files]
Source: "F:\temp\ImageViewer\*"; DestDir: "{app}"; Flags: recursesubdirs

[Icons]
Name: "{group}\ImageViewer"; Filename: "{app}\ImageViewer.exe"

[Code]
function InitializeSetup(): boolean;
var
  ResultCode: integer;
begin
  if Exec('java', '-version', '', SW_SHOW, ewWaitUntilTerminated, ResultCode) then begin
    Result := true;    
  end
  else begin          
    if MsgBox('This tool requires Java Runtime Environment version 1.8 or newer to run. Please download and install the JRE and run this setup again. Do you want to download it now?', mbConfirmation, MB_YESNO) = idYes then begin
      Result := false;
      ShellExec('open', 'https://java.com/download/', '', '', SW_SHOWNORMAL, ewNoWait, ResultCode);
    end;  
  end;
end;