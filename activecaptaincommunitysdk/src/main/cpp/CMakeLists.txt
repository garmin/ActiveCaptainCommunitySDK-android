cmake_minimum_required(VERSION 3.10.2)

project("activecaptaincommunitysdk")

add_definitions( -DSQLITE_ENABLE_FTS5 -DSQLITE_ENABLE_RTREE -DSQLITE_TEMP_STORE=3 )

include_directories(
             acdb/submodules/mustache
             acdb/submodules/rapidjson/include
             acdb/submodules/SQLiteCpp/include
             acdb/submodules/SQLiteCpp/sqlite3
             acdb/Include/Private
             acdb/Include/Public
             acdb/Navionics
             acdb/Navionics/Stubs
             acdb/Thirdparty/Include/Private
             acdb/Thirdparty/Navionics/Extensions )

add_library( # Sets the name of the library.
             activecaptaincommunitysdk

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             acdb/Adapters/InfoAdapter.cpp
             acdb/Adapters/MarkerAdapter.cpp
             acdb/Adapters/MergeAdapter.cpp
             acdb/Adapters/PresentationAdapter.cpp
             acdb/Adapters/TranslationAdapter.cpp
             acdb/Adapters/UpdateAdapter.cpp
             acdb/DTO/MapMarker.cpp
             acdb/DTO/MapMarkerFilter.cpp
             acdb/DTO/SearchMarker.cpp
             acdb/DTO/SearchMarkerFilter.cpp
             acdb/DTO/Version.cpp
             acdb/Json/JsonParser.cpp
             acdb/Json/MarkerParser.cpp
             acdb/Json/RequestWriter.cpp
             acdb/Json/ResponseParser.cpp
             acdb/Json/ReviewParser.cpp
             acdb/Json/WebViewResponseParser.cpp
             acdb/Navionics/NavDateTime.cpp
             acdb/Navionics/NavTimeSpan.cpp
             acdb/Presentation/Field/AttributeField.cpp
             acdb/Presentation/Field/AttributePriceField.cpp
             acdb/Presentation/Field/BusinessPhotoField.cpp
             acdb/Presentation/Field/BusinessPromotionField.cpp
             acdb/Presentation/Field/BusinessPromotionListField.cpp
             acdb/Presentation/Field/CompetitorAdField.cpp
             acdb/Presentation/Field/LinkField.cpp
             acdb/Presentation/Field/ResponseField.cpp
             acdb/Presentation/Field/ReviewField.cpp
             acdb/Presentation/Field/ReviewPhotoField.cpp
             acdb/Presentation/Field/ReviewSummary.cpp
             acdb/Presentation/Field/StringField.cpp
             acdb/Presentation/Field/YesNoMultiValueField.cpp
             acdb/Presentation/Field/YesNoPriceField.cpp
             acdb/Presentation/Field/YesNoUnknownNearbyField.cpp
             acdb/Presentation/Section/Address.cpp
             acdb/Presentation/Section/Amenities.cpp
             acdb/Presentation/Section/Business.cpp
             acdb/Presentation/Section/CompetitorAd.cpp
             acdb/Presentation/Section/Contact.cpp
             acdb/Presentation/Section/Dockage.cpp
             acdb/Presentation/Section/Fuel.cpp
             acdb/Presentation/Section/MarkerDetail.cpp
             acdb/Presentation/Section/Moorings.cpp
             acdb/Presentation/Section/Navigation.cpp
             acdb/Presentation/Section/Retail.cpp
             acdb/Presentation/Section/ReviewDetail.cpp
             acdb/Presentation/Section/Services.cpp
             acdb/Presentation/BusinessPhotoList.cpp
             acdb/Presentation/MustacheContext.cpp
             acdb/Presentation/MustacheViewFactory.cpp
             acdb/Presentation/PresentationMarker.cpp
             acdb/Presentation/PresentationMarkerFactory.cpp
             acdb/Presentation/ReviewList.cpp
             acdb/Queries/AddressQuery.cpp
             acdb/Queries/AmenitiesQuery.cpp
             acdb/Queries/BusinessPhotoQuery.cpp
             acdb/Queries/BusinessProgramQuery.cpp
             acdb/Queries/BusinessQuery.cpp
             acdb/Queries/CompetitorQuery.cpp
             acdb/Queries/ContactQuery.cpp
             acdb/Queries/DockageQuery.cpp
             acdb/Queries/FuelQuery.cpp
             acdb/Queries/LanguageQuery.cpp
             acdb/Queries/MarkerMetaQuery.cpp
             acdb/Queries/MarkerQuery.cpp
             acdb/Queries/MooringsQuery.cpp
             acdb/Queries/MustacheTemplateQuery.cpp
             acdb/Queries/NavigationQuery.cpp
             acdb/Queries/PositionQuery.cpp
             acdb/Queries/RetailQuery.cpp
             acdb/Queries/ReviewQuery.cpp
             acdb/Queries/ReviewPhotoQuery.cpp
             acdb/Queries/ReviewSummaryQuery.cpp
             acdb/Queries/SearchMarkerQuery.cpp
             acdb/Queries/ServicesQuery.cpp
             acdb/Queries/TileLastUpdateQuery.cpp
             acdb/Queries/TilesQuery.cpp
             acdb/Queries/TranslatorQuery.cpp
             acdb/Queries/VersionQuery.cpp
             acdb/submodules/SQLiteCpp/src/Backup.cpp
             acdb/submodules/SQLiteCpp/src/Column.cpp
             acdb/submodules/SQLiteCpp/src/Database.cpp
             acdb/submodules/SQLiteCpp/src/Exception.cpp
             acdb/submodules/SQLiteCpp/src/Statement.cpp
             acdb/submodules/SQLiteCpp/src/Transaction.cpp
             acdb/submodules/SQLiteCpp/sqlite3/sqlite3.c
             acdb/ThirdParty/Navionics/Extensions/NavDateTimeExtensions.cpp
             acdb/ThirdParty/DatabaseConfig.cpp
             acdb/ThirdParty/EventDispatcher.cpp
             acdb/ThirdParty/FileUtilPosix.cpp
             acdb/ThirdParty/ReadWriteLock.cpp
             acdb/AcdbUrlAction.cpp
             acdb/DataService.cpp
             acdb/ISettingsManager.cpp
             acdb/MarkerFactory.cpp
             acdb/Repository.cpp
             acdb/RwlLocker.cpp
             acdb/SectionType.cpp
             acdb/SettingsManager.cpp
             acdb/SqliteCppUtil.cpp
             acdb/StringFormatter.cpp
             acdb/StringUtil.cpp
             acdb/TableDataTypes.cpp
             acdb/TextTranslator.cpp
             acdb/UpdateService.cpp
        ActiveCaptainDatabaseJni.cpp)

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_link_libraries( # Specifies the target library.
                       activecaptaincommunitysdk

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )