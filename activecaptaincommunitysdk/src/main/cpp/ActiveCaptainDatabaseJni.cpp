/*------------------------------------------------------------------------------
Copyright 2021 Garmin Ltd. or its subsidiaries.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
------------------------------------------------------------------------------*/

#include <jni.h>
#include <memory>
#include <string>
#include "Acdb/AcdbUrlAction.hpp"
#include "Acdb/DataService.hpp"
#include "Acdb/Repository.hpp"
#include "Acdb/ISettingsManager.hpp"
#include "Acdb/StringUtil.hpp"
#include "Acdb/UpdateService.hpp"
#include "Acdb/Version.hpp"
#include "NavDateTimeExtensions.hpp"
#include "UTL_pub_lib_cnvt.h"

using DataServicePtr = std::shared_ptr<Acdb::IDataService>;
using UpdateServicePtr = std::shared_ptr<Acdb::IUpdateService>;

int getEnumValue(JNIEnv* env, const char* typeName, jobject enumValue) {
    jmethodID ordinalMethod = env->GetMethodID(env->FindClass(typeName), "ordinal", "()I");
    return env->CallIntMethod(enumValue, ordinalMethod);
}

jobject getJEnumValue(JNIEnv* env, std::string typeName, const char* value) {
    jclass clazz = env->FindClass(typeName.c_str());
    jfieldID fieldId = env->GetStaticFieldID(clazz, value, (std::string{"L"} + typeName + ";").c_str());
    jobject jValue = env->GetStaticObjectField(clazz, fieldId);

    return jValue;
}

std::string getString(JNIEnv* env, jobject obj, jstring jstr) {
    if (jstr != nullptr) {
        const char* chars = env->GetStringUTFChars(jstr, nullptr);
        std::string result{chars};
        env->ReleaseStringUTFChars(jstr, chars);

        return result;
    } else {
        return std::string{};
    }
}

struct SmartPointerHolder
{
    Acdb::RepositoryPtr repository;
    DataServicePtr dataService;
    UpdateServicePtr updateService;
};

jfieldID getPtrFieldId(JNIEnv* env, jobject obj) {
    static jfieldID ptrFieldId = 0;

    if (!ptrFieldId) {
        jclass c = env->GetObjectClass(obj);
        ptrFieldId = env->GetFieldID(c, "ptrHolder", "J");
        env->DeleteLocalRef(c);
    }

    return ptrFieldId;
}

extern "C" {
    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_init(JNIEnv* env, jobject obj, jstring databasePathJstr, jstring languageCodeJstr) {
        SmartPointerHolder* holder = new SmartPointerHolder;

        std::string databasePathStr = getString(env, obj, databasePathJstr);
        std::string languageCodeStr = getString(env, obj, languageCodeJstr);

        holder->repository.reset(new Acdb::Repository{databasePathStr});
        holder->repository->Open();
        holder->dataService.reset( new Acdb::DataService{holder->repository, languageCodeStr});
        holder->updateService.reset( new Acdb::UpdateService{holder->repository});

        env->SetLongField(obj, getPtrFieldId(env, obj), (jlong)holder);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_cleanup(JNIEnv* env, jobject obj) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        holder->repository->Close();

        env->SetLongField(obj, getPtrFieldId(env, obj), (jlong)0);
        delete holder;
    }


    // Repository functions

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_deleteDatabase(JNIEnv* env, jobject obj) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        holder->repository->Delete();
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_deleteTile(JNIEnv* env, jobject obj, jint tileX, jint tileY) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        Acdb::TileXY tileXY{tileX, tileY};
        holder->repository->DeleteTile(tileXY, true);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_deleteTileReviews(JNIEnv* env, jobject obj, jint tileX, jint tileY) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        Acdb::TileXY tileXY{tileX, tileY};
        holder->repository->DeleteTileReviews(tileXY);
    }

    jobject Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_getTileLastModified(JNIEnv* env, jobject obj, jint tileX, jint tileY) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        jclass clazz = env->FindClass(
                "com/garmin/marine/activecaptaincommunitysdk/DTO/LastUpdateInfoType");

        if (!clazz)
        {
            return nullptr;
        }

        jmethodID initMethodId = env->GetMethodID(clazz, "<init>",
                                              "(Ljava/lang/String;Ljava/lang/String;)V");

        if (!initMethodId)
        {
            return nullptr;
        }

        Acdb::TileXY tileXY{tileX, tileY};
        Acdb::LastUpdateInfoType lastUpdateInfo;

        holder->repository->GetTileLastUpdateInfo(tileXY, lastUpdateInfo);

        jstring markerLastUpdateJstr = nullptr;
        jstring reviewLastUpdateJstr = nullptr;

        if (lastUpdateInfo.mMarkerLastUpdate != 0) {
            Navionics::NavDateTime markerLastUpdate = Acdb::NavDateTimeExtensions::EpochToNavDateTime(Acdb::UNIX_EPOCH, lastUpdateInfo.mMarkerLastUpdate);
            std::string markerLastUpdateStr;
            markerLastUpdate.ToString(markerLastUpdateStr, YYYYMMDDTHHMMSSZ_FORMAT);
            markerLastUpdateJstr = env->NewStringUTF(markerLastUpdateStr.c_str());
        }

        if (lastUpdateInfo.mUserReviewLastUpdate != 0) {
            Navionics::NavDateTime reviewLastUpdate = Acdb::NavDateTimeExtensions::EpochToNavDateTime(Acdb::UNIX_EPOCH, lastUpdateInfo.mUserReviewLastUpdate);
            std::string reviewLastUpdateStr;
            reviewLastUpdate.ToString(reviewLastUpdateStr, YYYYMMDDTHHMMSSZ_FORMAT);
            reviewLastUpdateJstr = env->NewStringUTF(reviewLastUpdateStr.c_str());
        }

        return env->NewObject(clazz, initMethodId, markerLastUpdateJstr, reviewLastUpdateJstr);
    }

jobject Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_getTilesLastModifiedByBoundingBox(JNIEnv* env, jobject obj, jdouble south, jdouble west, jdouble north, jdouble east) {
    SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

    jclass tileXYClazz = env->FindClass(
            "com/garmin/marine/activecaptaincommunitysdk/DTO/TileXY");

    if (!tileXYClazz)
    {
        return nullptr;
    }

    jmethodID tileXYInitMethodId = env->GetMethodID(tileXYClazz, "<init>",
                                              "(II)V");

    if (!tileXYInitMethodId)
    {
        return nullptr;
    }

    jclass lastUpdateInfoTypeClazz = env->FindClass("com/garmin/marine/activecaptaincommunitysdk/DTO/LastUpdateInfoType");

    if (!lastUpdateInfoTypeClazz)
    {
        return nullptr;
    }

    jmethodID lastUpdateInfoTypeInitMethodId = env->GetMethodID(lastUpdateInfoTypeClazz, "<init>",
                                                                "(Ljava/lang/String;Ljava/lang/String;)V");

    if (!lastUpdateInfoTypeInitMethodId)
    {
        return nullptr;
    }

    jclass hashMapClazz = env->FindClass("java/util/HashMap");

    if (!hashMapClazz)
    {
        return nullptr;
    }

    jmethodID hashMapInitMethodId = env->GetMethodID(hashMapClazz, "<init>", "()V");
    jmethodID hashMapPutMethodId = env->GetMethodID(hashMapClazz, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    bbox_type bbox;
    bbox.nec.lat = (int32_t)north * UTL_DEG_TO_SEMI;
    bbox.nec.lon = (int32_t)east * UTL_DEG_TO_SEMI;
    bbox.swc.lat = (int32_t)south * UTL_DEG_TO_SEMI;
    bbox.swc.lon = (int32_t)west * UTL_DEG_TO_SEMI;

    std::vector<bbox_type> bboxes{bbox};
    std::map<Acdb::TileXY, Acdb::LastUpdateInfoType> lastUpdateInfos;

    holder->repository->GetTilesLastUpdateInfoByBoundingBoxes(bboxes, lastUpdateInfos);

    jobject result = env->NewObject(hashMapClazz, hashMapInitMethodId);

    for (std::map<Acdb::TileXY, Acdb::LastUpdateInfoType>::iterator it = lastUpdateInfos.begin(); it != lastUpdateInfos.end(); it++)
    {
        jstring markerLastUpdateJstr = nullptr;
        jstring reviewLastUpdateJstr = nullptr;

        if (it->second.mMarkerLastUpdate != 0) {
            Navionics::NavDateTime markerLastUpdate = Acdb::NavDateTimeExtensions::EpochToNavDateTime(Acdb::UNIX_EPOCH, it->second.mMarkerLastUpdate);
            std::string markerLastUpdateStr;
            markerLastUpdate.ToString(markerLastUpdateStr, YYYYMMDDTHHMMSSZ_FORMAT);
            markerLastUpdateJstr = env->NewStringUTF(markerLastUpdateStr.c_str());
        }

        if (it->second.mUserReviewLastUpdate != 0) {
            Navionics::NavDateTime reviewLastUpdate = Acdb::NavDateTimeExtensions::EpochToNavDateTime(Acdb::UNIX_EPOCH, it->second.mUserReviewLastUpdate);
            std::string reviewLastUpdateStr;
            reviewLastUpdate.ToString(reviewLastUpdateStr, YYYYMMDDTHHMMSSZ_FORMAT);
            reviewLastUpdateJstr = env->NewStringUTF(reviewLastUpdateStr.c_str());
        }

        jobject tileXY = env->NewObject(tileXYClazz, tileXYInitMethodId, it->first.mX, it->first.mY);
        jobject lastUpdateInfo = env->NewObject(lastUpdateInfoTypeClazz, lastUpdateInfoTypeInitMethodId, markerLastUpdateJstr, reviewLastUpdateJstr);
        env->CallObjectMethod(result, hashMapPutMethodId, tileXY, lastUpdateInfo);
    }

    return result;
}

    jstring Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_getVersion(JNIEnv* env, jobject obj) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        Acdb::Version version = holder->repository->GetVersion();

        return env->NewStringUTF(version.ToString().c_str());
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_installTile(JNIEnv* env, jobject obj, jstring pathJstr, jint tileX, jint tileY) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string pathStr = getString(env, obj, pathJstr);
        Acdb::TileXY tileXY{tileX, tileY};
        holder->repository->InstallSingleTileDatabase(pathStr, tileXY);
    }


    // DataService functions

    jobjectArray Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_getSearchMarkers(JNIEnv* env, jobject obj, jstring nameJstr, jdouble south, jdouble west, jdouble north, jdouble east, jint maxResultCount, jboolean escapeHtml) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        Acdb::SearchMarkerFilter filter;

        std::string nameStr = getString(env, obj, nameJstr);
        if (!nameStr.empty()) {
            filter.SetSearchString(nameStr);
        }

        bbox_type bbox;
        bbox.nec.lat = (int32_t)(north * UTL_DEG_TO_SEMI);
        bbox.nec.lon = (east == -180.0 || east == 180.0) ? INT32_MAX : (int32_t)(east * UTL_DEG_TO_SEMI);
        bbox.swc.lat = (int32_t)(south * UTL_DEG_TO_SEMI);
        bbox.swc.lon = (west == -180.0 || west == 180.0) ? INT32_MIN : (int32_t)(west * UTL_DEG_TO_SEMI);
        filter.SetBbox(bbox);

        filter.AddType(ACDB_ALL_TYPES);
        filter.AddCategory(Acdb::SearchMarkerFilter::Any);
        filter.SetMaxResults(maxResultCount);

        std::vector<Acdb::ISearchMarkerPtr> searchMarkers;
        holder->dataService->GetSearchMarkersByFilter(filter, searchMarkers);

        jclass clazz = env->FindClass("com/garmin/marine/activecaptaincommunitysdk/DTO/SearchMarker");
        if (!clazz)
        {
            return nullptr;
        }

        jmethodID initMethodId = env->GetMethodID(clazz, "<init>",
                                                  "(JLjava/lang/String;Lcom/garmin/marine/activecaptaincommunitysdk/DTO/MarkerType;DDLcom/garmin/marine/activecaptaincommunitysdk/DTO/MapIconType;)V");

        if (!initMethodId)
        {
            return nullptr;
        }

        jobjectArray results = env->NewObjectArray(searchMarkers.size(), clazz, nullptr);

        const char* markerTypeName = "com/garmin/marine/activecaptaincommunitysdk/DTO/MarkerType";

        const std::map<ACDB_type_type, jobject> MARKER_TYPES
        {
            {ACDB_UNKNOWN_TYPE, getJEnumValue(env, markerTypeName, "UNKNOWN")},
            {ACDB_ANCHORAGE, getJEnumValue(env, markerTypeName, "ANCHORAGE")},
            {ACDB_BOAT_RAMP, getJEnumValue(env, markerTypeName, "BOAT_RAMP")},
            {ACDB_BRIDGE, getJEnumValue(env, markerTypeName, "BRIDGE")},
            {ACDB_BUSINESS, getJEnumValue(env, markerTypeName, "BUSINESS")},
            {ACDB_DAM, getJEnumValue(env, markerTypeName, "DAM")},
            {ACDB_FERRY, getJEnumValue(env, markerTypeName, "FERRY")},
            {ACDB_HAZARD, getJEnumValue(env, markerTypeName, "HAZARD")},
            {ACDB_INLET, getJEnumValue(env, markerTypeName, "INLET")},
            {ACDB_LOCK, getJEnumValue(env, markerTypeName, "LOCK")},
            {ACDB_MARINA, getJEnumValue(env, markerTypeName, "MARINA")}
        };

        const char* mapIconTypeName = "com/garmin/marine/activecaptaincommunitysdk/DTO/MapIconType";

        const std::map<Acdb::MapIconType, jobject> MAP_ICON_TYPES
        {
            {Acdb::MapIconType::Unknown, getJEnumValue(env, mapIconTypeName, "UNKNOWN")},
            {Acdb::MapIconType::Anchorage, getJEnumValue(env, mapIconTypeName, "ANCHORAGE")},
            {Acdb::MapIconType::BoatRamp, getJEnumValue(env, mapIconTypeName, "BOAT_RAMP")},
            {Acdb::MapIconType::Bridge, getJEnumValue(env, mapIconTypeName, "BRIDGE")},
            {Acdb::MapIconType::Business, getJEnumValue(env, mapIconTypeName, "BUSINESS")},
            {Acdb::MapIconType::Dam, getJEnumValue(env, mapIconTypeName, "DAM")},
            {Acdb::MapIconType::Ferry, getJEnumValue(env, mapIconTypeName, "FERRY")},
            {Acdb::MapIconType::Hazard, getJEnumValue(env, mapIconTypeName, "HAZARD")},
            {Acdb::MapIconType::Inlet, getJEnumValue(env, mapIconTypeName, "INLET")},
            {Acdb::MapIconType::Lock, getJEnumValue(env, mapIconTypeName, "LOCK")},
            {Acdb::MapIconType::Marina, getJEnumValue(env, mapIconTypeName, "MARINA")},
            {Acdb::MapIconType::AnchorageSponsor, getJEnumValue(env, mapIconTypeName, "ANCHORAGE_SPONSOR")},
            {Acdb::MapIconType::BusinessSponsor, getJEnumValue(env, mapIconTypeName, "BUSINESS_SPONSOR")},
            {Acdb::MapIconType::MarinaSponsor, getJEnumValue(env, mapIconTypeName, "MARINA_SPONSOR")}
        };

        for(std::size_t i = 0; i < searchMarkers.size(); i++)
        {
            std::string markerName = searchMarkers[i]->GetName();
            if (escapeHtml == true) {
                Acdb::String::HtmlEscape(markerName);
            }

            std::map<ACDB_type_type, jobject>::const_iterator markerIt = MARKER_TYPES.find(searchMarkers[i]->GetType());
            if (markerIt == MARKER_TYPES.end())
            {
                markerIt = MARKER_TYPES.begin();
            }

            std::map<Acdb::MapIconType, jobject>::const_iterator iconIt = MAP_ICON_TYPES.find(searchMarkers[i]->GetMapIcon());
            if (iconIt == MAP_ICON_TYPES.end())
            {
                iconIt = MAP_ICON_TYPES.begin();
            }

            jobject result = env->NewObject(clazz, initMethodId, searchMarkers[i]->GetId(), env->NewStringUTF(markerName.c_str()), markerIt->second, searchMarkers[i]->GetPosition().lat * UTL_SEMI_TO_DEG, searchMarkers[i]->GetPosition().lon * UTL_SEMI_TO_DEG, iconIt->second);
            env->SetObjectArrayElement(results, i, result);
        }

        return results;
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_setHeadContent(JNIEnv* env, jobject obj, jstring headContentJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string headContentStr = getString(env, obj, headContentJstr);
        holder->dataService->SetHeadContent(headContentStr);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_setImagePrefix(JNIEnv* env, jobject obj, jstring imagePrefixJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string imagePrefixStr = getString(env, obj, imagePrefixJstr);
        holder->dataService->SetImagePrefix(imagePrefixStr);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_setLanguage(JNIEnv* env, jobject obj, jstring languageCodeJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string languageCodeStr = getString(env, obj, languageCodeJstr);
        holder->dataService->SetLanguage(languageCodeStr);
    }


    // ISettingsManager functions

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_setCoordinateFormat(JNIEnv* env, jobject obj, jobject coordinateFormat) {
        int value = getEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/CoordinateFormatType", coordinateFormat);
        Acdb::ISettingsManager::GetISettingsManager().SetCoordinateFormat((ACDB_coord_format_type)value);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_setDateFormat(JNIEnv* env, jobject obj, jobject dateFormat) {
        int value = getEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DateFormatType", dateFormat);
        Acdb::ISettingsManager::GetISettingsManager().SetDateFormat((ACDB_date_format_type)value);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_setDistanceUnit(JNIEnv* env, jobject obj, jobject distanceUnit) {
        int value = getEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DistanceUnit", distanceUnit);

        const std::map<int, ACDB_unit_type> DISTANCE_UNITS{
            {0, ACDB_UNKNOWN_UNIT},
            {1, ACDB_FEET},
            {2, ACDB_METER}
        };

        std::map<int, ACDB_unit_type>::const_iterator unitIt = DISTANCE_UNITS.find(value);
        if (unitIt == DISTANCE_UNITS.end())
        {
            unitIt = DISTANCE_UNITS.begin();
        }

        Acdb::ISettingsManager::GetISettingsManager().SetDistanceUnit(unitIt->second);
    }


    // UpdateService functions

    jlong Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_processCreateMarkerResponse(JNIEnv* env, jobject obj, jstring jsonJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string jsonStr = getString(env, obj, jsonJstr);
        ACDB_marker_idx_type markerIdx;
        holder->updateService->ProcessCreateMarkerResponse(jsonStr, markerIdx);

        return (jlong)markerIdx;
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_processMoveMarkerResponse(JNIEnv* env, jobject obj, jstring jsonJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string jsonStr = getString(env, obj, jsonJstr);
        holder->updateService->ProcessMoveMarkerResponse(jsonStr);
    }

    jint Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_processSyncMarkersResponse(JNIEnv* env, jobject obj, jstring jsonJstr, jint tileX, jint tileY) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::size_t resultCount;

        std::string jsonStr = getString(env, obj, jsonJstr);
        Acdb::TileXY tileXY{tileX, tileY};
        holder->updateService->ProcessSyncMarkersResponse(jsonStr, tileXY, resultCount);

        return resultCount;
    }

    jint Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_processSyncReviewsResponse(JNIEnv* env, jobject obj, jstring jsonJstr, jint tileX, jint tileY) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::size_t resultCount;

        std::string jsonStr = getString(env, obj, jsonJstr);
        Acdb::TileXY tileXY{tileX, tileY};
        holder->updateService->ProcessSyncReviewsResponse(jsonStr, tileXY, resultCount);

        return resultCount;
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_processVoteForReviewResponse(JNIEnv* env, jobject obj, jstring jsonJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string jsonStr = getString(env, obj, jsonJstr);
        holder->updateService->ProcessVoteForReviewResponse(jsonStr);
    }

    void Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_processWebViewResponse(JNIEnv* env, jobject obj, jstring jsonJstr) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string jsonStr = getString(env, obj, jsonJstr);
        holder->updateService->ProcessWebViewResponse(jsonStr);
    }


    // AcdbUrlAction

    jobject Java_com_garmin_marine_activecaptaincommunitysdk_ActiveCaptainDatabase_parseAcdbUrl(JNIEnv* env, jobject obj, jstring urlJstr, jstring captainNameJstr, jint pageSize) {
        SmartPointerHolder* holder = (SmartPointerHolder*) env->GetLongField(obj, getPtrFieldId(env, obj));

        std::string captainNameStr = getString(env, obj, captainNameJstr);
        std::string urlStr = getString(env, obj, urlJstr);
        Acdb::AcdbUrlActionPtr action;
        std::string contentStr;
        jobject jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "UNKNOWN");

        jclass clazz = env->FindClass(
                "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction");

        if (!clazz)
        {
            return nullptr;
        }

        jmethodID initMethodId = env->GetMethodID(clazz, "<init>",
                                                  "(Lcom/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType;Ljava/lang/String;)V");

        if (!initMethodId)
        {
            return nullptr;
        }

        if (Acdb::ParseAcdbUrl(urlStr, action))
        {
            switch (action->GetAction())
            {
                case Acdb::AcdbUrlAction::ActionType::SeeAll:
                {
                    jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "SEE_ALL");

                    Acdb::SeeAllAction* seeAllAction = static_cast<Acdb::SeeAllAction*>(action.get());

                    if (Acdb::IsReviewsSection(seeAllAction->GetSection())) {
                        contentStr = holder->dataService->GetReviewListHtml(seeAllAction->GetMarkerId(), seeAllAction->GetPageNumber(), pageSize, captainNameStr);
                    } else {
                        contentStr = holder->dataService->GetSectionPageHtml(seeAllAction->GetMarkerId(), seeAllAction->GetSection());
                    }

                    break;
                }
                case Acdb::AcdbUrlAction::ActionType::ShowPhotos:
                {
                    jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "SHOW_PHOTOS");

                    Acdb::ShowPhotosAction* showPhotosAction = static_cast<Acdb::ShowPhotosAction*>(action.get());
                    contentStr = holder->dataService->GetBusinessPhotoListHtml(showPhotosAction->GetMarkerId());

                    break;
                }
                case Acdb::AcdbUrlAction::ActionType::ShowSummary:
                {
                    jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "SHOW_SUMMARY");

                    Acdb::ShowSummaryAction* showSummaryAction = static_cast<Acdb::ShowSummaryAction*>(action.get());
                    contentStr = holder->dataService->GetPresentationMarkerHtml(showSummaryAction->GetMarkerId(), captainNameStr);

                    break;
                }
                case Acdb::AcdbUrlAction::ActionType::Edit:
                {
                    jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "EDIT");

                    Acdb::EditAction* editAction = static_cast<Acdb::EditAction*>(action.get());
                    contentStr = editAction->GetUrl();

                    break;
                }
                case Acdb::AcdbUrlAction::ActionType::ReportReview:
                {
                    jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "REPORT_REVIEW");

                    Acdb::ReportReviewAction* reportReviewAction = static_cast<Acdb::ReportReviewAction*>(action.get());
                    contentStr = reportReviewAction->GetUrl();

                    break;
                }
                case Acdb::AcdbUrlAction::ActionType::VoteReview:
                {
                    jActionType = getJEnumValue(env, "com/garmin/marine/activecaptaincommunitysdk/DTO/AcdbUrlAction$ActionType", "VOTE_REVIEW");

                    Acdb::VoteReviewAction* voteAction = static_cast<Acdb::VoteReviewAction*>(action.get());
                    contentStr = std::to_string(voteAction->GetReviewId());

                    break;
                }
            }
        }

        if (!contentStr.empty()) {
            return env->NewObject(clazz, initMethodId, jActionType, env->NewStringUTF(contentStr.c_str()));
        } else {
            return nullptr;
        }
    }
}