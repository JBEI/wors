'use strict';

angular.module('ice.entry.controller', ['ui.bootstrap'])
    .controller('EntryAttachmentController', function ($scope, $window, $stateParams, Util) {
        Util.list("rest/parts/" + $stateParams.id + "/attachments", function (result) {
            $scope.attachments = result;
        });

        $scope.downloadAttachment = function (attachment) {
            // todo
            //$window.open("rest/file/attachment/" + attachment.fileId + "?sid=" + $cookieStore.get("sessionId"), "_self");
        };
    })
    .controller('EntryCommentController', function ($scope, $stateParams, Util) {
        let entryId = $stateParams.id;
        $scope.newComment = {samples: []};

        Util.list('rest/parts/' + entryId + '/comments', function (result) {
            $scope.entryComments = result;
        });

        Util.list('rest/parts/' + entryId + '/samples', function (result) {
            $scope.entrySamples = result;
        });
    })
    .controller('ShotgunSequenceController', function ($scope, $window, $stateParams, $uibModal, Util) {
        let entryId = $stateParams.id;
        $scope.shotgunUploadError = undefined;
        $scope.maxSize = 5;
        $scope.shotgunParams = {limit: 5, currentPage: 1, start: 0};

        Util.list('rest/parts/' + entryId + '/shotgunsequences', function (result) {
            $scope.shotgunSequences = result;
        }, $scope.shotgunParams);

        $scope.shotgunPageChanged = function () {
            $scope.shotgunParams.start = ($scope.shotgunParams.currentPage - 1) * $scope.shotgunParams.limit;
            Util.list("/rest/parts/" + entryId + "/shotgunsequences", function (result) {
                $scope.shotgunSequences = result;
            }, $scope.shotgunParams);
        };

        $scope.downloadShotgunFile = function (sequence) {
            //$window.open("rest/file/shotgunsequence/" + sequence.fileId + "?sid=" + $cookieStore.get("sessionId"), "_self");
        };
    })
    .controller('EntryExperimentController', function ($scope, $stateParams, Util) {
        let entryId = $stateParams.id;
        $scope.experiment = {};
        $scope.addExperiment = false;

        Util.list("/rest/parts/" + entryId + "/experiments", function (result) {
            $scope.entryExperiments = result;
        });
    })
    .controller('EntryController', function ($scope, $stateParams, $location, $rootScope, $route, $window, $document,
                                             $uibModal, EntryService, Selection, Util) {
            let data;

            let getCopyFunction = function () {
                return function (event, copiedSequenceData, editorState) {
                    const clipboardData = event.clipboardData || window.clipboardData || event.originalEvent.clipboardData;
                    clipboardData.setData('text/plain', copiedSequenceData.sequence);
                    data.selection = editorState.selectionLayer;
                    data.openVECopied = copiedSequenceData;
                    clipboardData.setData('application/json', JSON.stringify(data));
                    event.preventDefault();
                }
            };

            let getPropProps = function () {
                return {
                    propertiesList: [
                        "features",
                        "translations",
                        "cutsites",
                        "orfs"
                    ]
                }
            };

            let getToolBarProps = function () {
                return {
                    toolList: [
                        "cutsiteTool",
                        "featureTool",
                        "orfTool",
                        "findTool",
                        "visibilityTool"
                    ]
                }
            };

            let getPanelsShown = function () {
                return [
                    [
                        {
                            id: "circular",
                            name: "Plasmid",
                            active: true
                        },

                        {
                            id: "rail",
                            name: "Linear Map",
                            active: false
                        },
                        {
                            id: "properties",
                            name: "Properties",
                            active: false
                        }
                    ]
                ]
            };

            let createVectorEditor = function (domNode, editorData, split, data) {
                $scope.loadMessage = "Rendering";

                let veEditor = $window.createVectorEditor(domNode, editorData);
                let panels = getPanelsShown();
                if (split) {
                    panels.push(
                        [
                            {
                                id: "sequence",
                                name: "Sequence Map",
                                active: true
                            }
                        ]);
                } else {
                    panels[0].push({
                        id: "sequence",
                        name: "Sequence Map",
                        active: false
                    })
                }

                veEditor.updateEditor({
                    readOnly: true,
                    sequenceData: data.sequenceData,
                    annotationVisibility: {
                        parts: false,
                        orfs: false,
                        cutsites: false
                    },
                    annotationsToSupport: {
                        features: true,
                        translations: true,
                        parts: false,
                        orfs: true,
                        cutsites: true,
                        primers: false
                    },
                    panelsShown: panels
                });
                return veEditor;
            };

            //
            // init
            //

            $scope.loadMessage = "Fetching";
            $scope.showSBOL = true;

            let params = $location.search();

            // get the entry general information
            let getGeneralInformation = function (recordId) {
                Util.get("rest/search/" + recordId,
                    function (result) {

                        $scope.sequence = result.sequence;
                        $scope.source = result.partSource;
                        $scope.entry = EntryService.convertToUIForm(result.part);
                        $scope.children = result.children;
                        $scope.entryFields = EntryService.getFieldsForType(result.part.type.toLowerCase());

                        // get sample count, comment count etc
                        // todo
                        //Util.get("rest/parts/" + $stateParams.id + "/statistics", function (stats) {
                        //    $scope.entryStatistics = stats;
                        //}, params);

                    }, params, function (error) {
                        if (error.status === 404)
                            $scope.notFound = true;
                        else if (error.status === 403)
                            $scope.noAccess = true;
                    });
            };
            getGeneralInformation($stateParams.id);

            // TODO : duplicate. find a way to combine with entryDirective
            $scope.openSequenceInVectorEditor = function () {
                let data = {
                    sequenceData: {
                        sequence: $scope.sequence.sequence, features: [], name: $scope.sequence.name
                    },
                    registryData: {
                        uri: $scope.sequence.uri,
                        identifier: $scope.sequence.identifier,
                        name: $scope.sequence.name,
                        circular: $scope.sequence.isCircular
                    }
                };

                for (let i = 0; i < $scope.sequence.features.length; i += 1) {
                    let feature = $scope.sequence.features[i];
                    if (!feature.locations.length)
                        continue;

                    let notes = feature.notes.length ? feature.notes[0].value : "";

                    for (let j = 0; j < feature.locations.length; j += 1) {
                        let location = feature.locations[j];

                        data.sequenceData.features.push({
                            start: location.genbankStart - 1,
                            end: location.end - 1,
                            fid: feature.id,
                            forward: feature.strand === 1,
                            type: feature.type,
                            name: feature.name,
                            notes: notes,
                            annotationType: feature.type,
                        })
                    }
                }

                let veEditor = createVectorEditor("createDomNodeForMe", {
                    editorName: "fullscreen-vector-editor",
                    isFullscreen: true,
                    doNotUseAbsolutePosition: true,
                    handleFullscreenClose: function () { // this will make the editor fullscreen by default, and will allow you to handle the close request
                        veEditor.close();                // handle vector editor root removal and clean up
                    },
                    onCopy: getCopyFunction(),
                    PropertiesProps: getPropProps(),
                    ToolBarProps: getToolBarProps()
                }, true, data);
            };

            $scope.getSequenceSectionHeader = function () {
                if ($scope.entry.hasSequence && !$scope.entry.basePairCount)
                    return "SBOL INFORMATION";
                return "SEQUENCE";
            };

            $scope.entryFields = undefined;
            $scope.entry = undefined;
            $scope.notFound = undefined;
            $scope.noAccess = undefined;

            let menuSubDetails = $scope.subDetails = EntryService.getMenuSubDetails();

            $scope.showSelection = function (index) {
                angular.forEach(menuSubDetails, function (details) {
                    details.selected = false;
                });
                menuSubDetails[index].selected = true;
                $scope.selection = menuSubDetails[index].url;
                if (menuSubDetails[index].id) {
                    $location.path("entry/" + $stateParams.id + "/" + menuSubDetails[index].id);
                } else {
                    $location.path("entry/" + $stateParams.id);
                }
            };

            // check if a selection has been made
            let menuOption = $stateParams.option;
            if (menuOption === undefined) {
                $scope.selection = menuSubDetails[0].url;
                menuSubDetails[0].selected = true;
            } else {
                menuSubDetails[0].selected = false;
                for (let i = 1; i < menuSubDetails.length; i += 1) {
                    if (menuSubDetails[i].id === menuOption) {
                        $scope.selection = menuSubDetails[i].url;
                        menuSubDetails[i].selected = true;
                        break;
                    }
                }

                if ($scope.selection === undefined) {
                    $scope.selection = menuSubDetails[0].url;
                    menuSubDetails[0].selected = true;
                }
            }

            $scope.displayForBLSValue = function (bslValue) {
                if (bslValue === -1) {
                    return "Restricted";
                }

                return bslValue;
            };

            // converts an array of string (currently only for autoCompleteAdd) to object so it can be edited
            $scope.checkConvertFieldToObject = function (field) {
                $scope.convertedAutoCompleteAdd = [];
                if (!angular.isArray($scope.entry[field.schema]))
                    return;

                if (field.inputType !== 'autoCompleteAdd')
                    return;

                for (let i = 0; i < $scope.entry[field.schema].length; i += 1) {
                    $scope.convertedAutoCompleteAdd[i] = {value: $scope.entry[field.schema][i]};
                }
            };

            $scope.nextEntryInContext = function () {
                console.log($rootScope.search);

                $rootScope.search.parameters.start += 1;
                $rootScope.search.parameters.retrieveCount = 1;

                Util.post("rest/search", $rootScope.search, function (result) {
                    let recordId = result.results[0].entryInfo.recordId;
                    getGeneralInformation(recordId);
                    $location.path("/search/entry/" + recordId, false);


                    //$rootScope.searchResults = result;
                    //$scope.loadingSearchResults = false;
                }, {}, function () {
                    //$scope.loadingSearchResults = false;
                    //$rootScope.searchResults = undefined;
                });

                //$scope.context.offset += 1;
                //$scope.context.callback($scope.context.offset, function (result) {
                //    $location.path("entry/" + result);
                //});
            };

            $scope.prevEntryInContext = function () {
                if ($rootScope.search.parameters.start === 0)
                    return;

                $rootScope.search.parameters.start -= 1;
                $rootScope.search.parameters.retrieveCount = 1;

                Util.post("rest/search", $rootScope.search, function (result) {
                    let recordId = result.results[0].entryInfo.recordId;
                    //getGeneralInformation(recordId);
                    $location.path("/search/entry/" + recordId, false);

                    //$rootScope.searchResults = result;
                    //$scope.loadingSearchResults = false;
                }, {}, function () {
                    //$scope.loadingSearchResults = false;
                    //$rootScope.searchResults = undefined;
                });
            };

            $scope.backToResults = function () {
                $location.path("/search/results");
            };
        }
    );
