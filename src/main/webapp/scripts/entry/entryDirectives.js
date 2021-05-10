'use strict';

angular.module('ice.entry.directives', [])
    .directive("icePlate96", function () {
        return {
            scope: {
                sample: "=",
                delete: "&onDelete",
                remote: "=",
                entry: "=",
                selected: "="
            },

            restrict: "E",
            templateUrl: "scripts/entry/sample/plate96.html",
            controller: "DisplaySampleController"
        }
    })
    .directive("iceShelf", function () {
        return {
            scope: {
                sample: "=",
                delete: "&onDelete",
                remote: "="
            },

            restrict: "E",
            templateUrl: "scripts/entry/sample/shelf.html",
            controller: "DisplaySampleController"
        }
    })
    .directive("iceGeneric", function () {
        return {
            scope: {
                sample: "=",
                delete: "&onDelete",
                remote: "="
            },

            restrict: "E",
            templateUrl: "scripts/entry/sample/generic.html",
            controller: "DisplaySampleController"
        }
    })
    .directive("iceAddgene", function () {
        return {
            scope: {
                sample: "=",
                delete: "&onDelete",
                remote: "="
            },

            restrict: "E",
            templateUrl: "scripts/entry/sample/addgene.html",
            controller: "DisplaySampleController"
        }
    })
    .directive("iceVectorViewer", function () {
        return {
            scope: {
                entry: "=",
                sequence: "="
            },

            restrict: "E",

            link: function (scope, element, attrs) {
            },

            template: '<div id="ve-Root" style="height: 550px"><br><img src="img/loader-mini.gif"> {{$scope.loadMessage || "Loading"}} sequence&hellip;</div>',

            controller: function ($rootScope, $scope, Util, $window) {
                if (!$scope.sequence || !$scope.sequence.sequence)
                    return;

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

                let createVectorEditor = function (domNode, editorData, split) {
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
                        //name the tools you want to see in the toolbar in the order you want to see them
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

                let convertToOpenVEModel = function (result) {
                    data = {
                        sequenceData: {
                            sequence: result.sequence,
                            features: [],
                            name: result.name,
                            circular: result.isCircular
                        },
                        registryData: {
                            uri: result.uri,
                            identifier: result.identifier,
                            name: result.name,
                            circular: result.isCircular
                        }
                    };

                    data.sequenceData.features = convertFeaturedDNASequence(result);
                    return result;
                };

                let convertFeaturedDNASequence = function (result) {
                    let features = [];

                    for (let i = 0; i < result.features.length; i += 1) {
                        let feature = result.features[i];
                        if (!feature.locations.length)
                            continue;

                        let notes = feature.notes.length ? feature.notes[0].value : "";

                        for (let j = 0; j < feature.locations.length; j += 1) {
                            let location = feature.locations[j];

                            let featureObject = {
                                start: location.genbankStart - 1,
                                end: location.end - 1,
                                fid: feature.id,
                                forward: feature.strand === 1,
                                type: feature.type,
                                name: feature.name,
                                notes: notes,
                                annotationType: feature.type,
                            };

                            features.push(featureObject);
                        }
                    }

                    return features;
                };

                //
                // init
                //
                $scope.loadMessage = "Processing";
                // $scope.sequenceName = $scope.sequence.name;

                // convert to model that open ve can work with
                convertToOpenVEModel($scope.sequence);
                let editorData = {
                    editorName: "vector-editor",
                    doNotUseAbsolutePosition: true,
                    onCopy: getCopyFunction(),
                    PropertiesProps: getPropProps(),
                    ToolBarProps: getToolBarProps()
                };

                createVectorEditor(document.getElementById("ve-Root"), editorData, false);
            }
        };
    });
