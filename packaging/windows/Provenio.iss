; Windows installer for Provenio, built with Inno Setup 6 from the app folder Gradle creates.
;
;   iscc /DAppVersion=0.40.123 /DSourceDir=<app folder> /DOutputDir=<dist> [/DIconFile=<ico>] Provenio.iss
;
; Updates run this same installer over the existing install: it replaces the app's files in place,
; never the user's data, which lives outside the install folder.

#ifndef AppVersion
  #error AppVersion is required, for example /DAppVersion=0.40.123
#endif
#ifndef SourceDir
  #error SourceDir is required: the Provenio folder from createReleaseDistributable
#endif
#ifndef OutputDir
  #define OutputDir "."
#endif

#define AppName "Provenio"
#define AppExe "Provenio.exe"
#define AppMutexName "ProvenioAppMutex"
#define SetupMutexName "ProvenioSetupMutex"
#define MsiUpgradeCode "{6F3C2B1E-8D4A-4E7B-9C15-2A7D0E5B9F43}"

[Setup]
AppId={{4FD7D767-40D1-4639-9A54-71B2A51ED8E7}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppName}
AppPublisherURL=https://github.com/Dimitrysaf/provenio
AppSupportURL=https://github.com/Dimitrysaf/provenio/issues
AppUpdatesURL=https://github.com/Dimitrysaf/provenio/releases
VersionInfoVersion={#AppVersion}
DefaultDirName={autopf}\{#AppName}
DisableDirPage=auto
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#OutputDir}
OutputBaseFilename=Provenio-Setup
#ifdef IconFile
SetupIconFile={#IconFile}
#endif
UninstallDisplayIcon={app}\{#AppExe}
UninstallDisplayName={#AppName}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
CloseApplications=force
RestartApplications=no
SetupMutex={#SetupMutexName}

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[InstallDelete]
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
Filename: "{app}\{#AppExe}"; Parameters: "--after-update"; Flags: nowait; Check: ShouldRelaunch

[Code]
var
  InstallFinished: Boolean;

function MsiEnumRelatedProducts(UpgradeCode: String; Reserved: Cardinal; Index: Cardinal; ProductCode: String): Cardinal;
  external 'MsiEnumRelatedProductsW@msi.dll stdcall';

{ True when the app started this installer to update itself and wants to be opened again. }
function ShouldRelaunch: Boolean;
begin
  Result := ExpandConstant('{param:RELAUNCH|0}') = '1';
end;

{ Waits up to 30 seconds for the app that started the update to close. }
procedure WaitForRunningApp;
var
  Waited: Integer;
begin
  Waited := 0;
  while CheckForMutexes('{#AppMutexName}') and (Waited < 30000) do
  begin
    Sleep(250);
    Waited := Waited + 250;
  end;
end;

{ Removes the MSI install older versions used, so the two never sit side by side. }
procedure RemoveMsiInstall;
var
  ProductCode: String;
  ResultCode: Integer;
begin
  ProductCode := StringOfChar(' ', 39);
  if MsiEnumRelatedProducts('{#MsiUpgradeCode}', 0, 0, ProductCode) = 0 then
  begin
    ProductCode := Copy(ProductCode, 1, 38);
    Exec(ExpandConstant('{sys}\msiexec.exe'), '/x ' + ProductCode + ' /qn /norestart', '',
      SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;

function InitializeSetup: Boolean;
begin
  if WizardSilent then WaitForRunningApp;
  Result := True;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  RemoveMsiInstall;
  Result := '';
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssDone then InstallFinished := True;
end;

{ Reopens the app with an error when an update it started did not finish. }
procedure DeinitializeSetup;
var
  AppPath: String;
  ResultCode: Integer;
begin
  if InstallFinished or not ShouldRelaunch then Exit;
  try
    AppPath := ExpandConstant('{app}\{#AppExe}');
    if FileExists(AppPath) then
      Exec(AppPath, '--update-failed=setup', '', SW_SHOW, ewNoWait, ResultCode);
  except
  end;
end;
