// ColorProfile.cpp : implements the routines from the com_jakabobnar_color_ColorProfileLoader.h

#include "stdafx.h"
#include <Windows.h>
#include <Icm.h>
#include "com_jakabobnar_colorprofile_WinColorProfileLoader.h"

#define PATH_SIZE 1024
#define MAX_PROFILES 150

/*
* Returns the file system path of the folder which contains all system color profiles.
* By default this is C:\Window\System32\spool\drivers\color
*/
JNIEXPORT jstring JNICALL Java_com_jakabobnar_colorprofile_WinColorProfileLoader_getColorProfilesLocation
	(JNIEnv *env, jobject thisObj)
{
    WCHAR szPath[PATH_SIZE];
    DWORD size;
    jstring path;
    char ret[PATH_SIZE];
    size_t s;

    size = sizeof(WCHAR)* PATH_SIZE;
    GetColorDirectory(NULL, szPath, &size);
    
    wcstombs_s(&s, ret, PATH_SIZE-1, szPath, sizeof(WCHAR)*PATH_SIZE-1);
    path = env->NewStringUTF(ret);
    return path;
}

/*
* Returns an array of profile files, one for each monitor attached to the computer. 
* The profiles are returned ordered: 1st element is for the 1st monitor, 2nd element 
* in the array is for the 2nd monitor and so on. All files are located in the folder 
* returned by the routine above.
*/
JNIEXPORT jobjectArray JNICALL Java_com_jakabobnar_colorprofile_WinColorProfileLoader_getColorProfiles
	(JNIEnv *env, jobject thisObj)
{
    WCHAR szPath[MAX_PATH];
    char ret[MAX_PROFILES][MAX_PATH];
    DISPLAY_DEVICE dd;
    dd.cb = sizeof(DISPLAY_DEVICE);
    jobjectArray result;
    DWORD deviceNum = 0;
    int i,c;
    size_t s;
    
    c = 0;
    //first get the display name
    while (EnumDisplayDevices(NULL, deviceNum, &dd, 0)) 
    {
        DISPLAY_DEVICE newdd = { 0 };
        newdd.cb = sizeof(DISPLAY_DEVICE);
        DWORD monitorNum = 0;
        // now fetch all monitors on this display
        while (EnumDisplayDevices(dd.DeviceName, monitorNum, &newdd, EDD_GET_DEVICE_INTERFACE_NAME))
        {
            monitorNum++;
            if (WcsGetDefaultColorProfile(WCS_PROFILE_MANAGEMENT_SCOPE_CURRENT_USER, newdd.DeviceKey, CPT_ICC, 
                    CPST_RGB_WORKING_SPACE, 1, MAX_PATH * sizeof(WCHAR), szPath))
            {
                wcstombs_s(&s, ret[c], MAX_PATH, szPath, MAX_PATH);
            }
            else 
            {
                ret[c][0] = 'N';
                ret[c][1] = '/';
                ret[c][2] = 'A';
                ret[c][3] = '\0';
            }
            c++;
            if (c >= MAX_PROFILES)
            {
                break;
            }
        }
        deviceNum++;
        if (c >= MAX_PROFILES)
        {
            break;
        }
    }
    result = env->NewObjectArray(c, env->FindClass("java/lang/String"),env->NewStringUTF(""));
    for (i = 0; i < c; i++)
    {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(ret[i]));
    }

    return result;
}
