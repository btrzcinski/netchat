!include MUI.nsh

!define PRODUCT_NAME "NetChat"
!define PRODUCT_VERSION "1.1a"
!define PRODUCT_PUBLISHER "NetChat, Ltd."


!define NETCHATDIR "C:\Documents and Settings\Andy Street\My Documents\netchat\trunk\src\j-client"
!define JREURL "http://axuric.xelocy.com:8080/NCResources/jre-6u2-windows-i586-p-iftw.exe"
!define TEMP $R0

!define MUI_ABORTWARNING

Name "NetChat"
OutFile "C:\Documents and Settings\Andy Street\Desktop\netchat-win32-installer.exe"
InstallDir "$PROGRAMFILES\NetChat"

!define MUI_LICENSEPAGE_RADIOBUTTONS

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "C:\Documents and Settings\Andy Street\My Documents\netchat\trunk\src\j-client\win32-installer\GNU PUBLIC LICENSE.rtf"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

Section "Main" mainSec
       Var /GLOBAL USERTYPE
       userInfo::getAccountType
       pop $USERTYPE
       
       ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" CurrentVersion
       IntCmp $0 "1.6" InstallNetchat InstallJava InstallNetchat
       Exit:
            Abort
       InstallJava:
            MessageBox MB_YESNOCANCEL "Setup has determined that you do not have the most recent\
             Java Runtime Environment installed on your system (Your version: $0).  Setup will now try to install Sun Microsystem's JRE 6 32-bit on your system.\
               If you are sure that you do have Java 5 or higher on your system, hit Cancel.  Continue?"\
            IDNO Exit IDCANCEL InstallNetchat

            StrCmp $USERTYPE "Admin" +3 0
            MessageBox MB_OK "You must be an admin to do this."
            Goto Exit
            
            DetailPrint "Downloading JRE..."
            NSISdl::download ${JREURL} "$TEMP\jre-6-online.exe"
            Pop $0
            StrCmp $0 "success" InstallJRE 0
            StrCmp $0 "cancel" 0 +3
            MessageBox MB_OK "Download exited on user cancel."
            Goto Exit
            MessageBox MB_OK "Setup encountered an error downloading the JRE."
            Goto Exit
       InstallJRE:
            DetailPrint "Installing JRE 6..."
            ExecWait "$TEMP\jre-6-online.exe" $1
            Delete "$TEMP\jre-6-online.exe"
            StrCmp $1 "0" InstallNetChat 0
            DetailPrint "Finished installing JRE 6..."
            MessageBox MB_OK "JRE setup exited abnormally."
            Goto Exit
       InstallNetchat:
            DetailPrint "Installing NetChat into $INSTDIR"
            SetOutPath $INSTDIR
            File /r "${NETCHATDIR}\win32-dist\*.*"
            File "${NETCHATDIR}\netchat-orig.ico"
            CreateShortCut "$DESKTOP\NetChat.lnk" "$PROGRAMFILES\NetChat\jclient.jar" ""\
                  "$INSTDIR\netchat-orig.ico"
            CreateShortCut "$SMPROGRAMS\NetChat.lnk" "$PROGRAMFILES\NetChat\jclient.jar" ""\
                  "$INSTDIR\netchat-orig.ico"
            WriteUninstaller "$INSTDIR\uninstall.exe"

            StrCmp $USERTYPE "Admin" 0 +3
            WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\NetChat"\
                  "DisplayName" "NetChat J-Client"
            WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\NetChat"\
                  "UninstallString" "$INSTDIR\uninstall.exe"
SectionEnd

Section "uninstall"
        MessageBox MB_OKCANCEL "Are you sure you want to remove NetChat \
              and all of its componenets from your system?" IDCANCEL 0 IDOK +2
        Quit
        DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\NetChat"
        RMDir /r /REBOOTOK $INSTDIR
        delete "$DESKTOP\NetChat.lnk"
        delete "$SMPROGRAMS\NetChat.lnk"
SectionEnd