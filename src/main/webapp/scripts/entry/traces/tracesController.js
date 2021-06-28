'use strict';

angular.module('ice.entry.traces.controller', [])
    .controller('TraceSequenceController', function ($scope, $window, $stateParams, $uibModal, Util) {
        let entryId = $stateParams.id;

        $scope.traceUploadError = undefined;
        $scope.maxSize = 5;
        $scope.tracesParams = {limit: 5, currentPage: 1, start: 0};

        Util.get("/rest/parts/" + entryId + "/traces", function (result) {
            $scope.traces = result;
        }, $scope.tracesParams);

        $scope.tracesPageChanged = function () {
            $scope.tracesParams.start = ($scope.tracesParams.currentPage - 1) * $scope.tracesParams.limit;
            Util.get("/rest/parts/" + entryId + "/traces", function (result) {
                $scope.traces = result;
            }, $scope.tracesParams);
        };

        $scope.showAddSangerTraceModal = function () {
            let modalInstance = $uibModal.open({
                templateUrl: "scripts/entry/modal/add-sanger-trace.html",
                controller: 'TraceSequenceUploadModalController',
                backdrop: 'static',
                resolve: {
                    entryId: function () {
                        return $stateParams.id;
                    }
                }
            });

            modalInstance.result.then(function () {
                $scope.tracesParams.start = 0;

                Util.get("/rest/parts/" + entryId + "/traces", function (result) {
                    Util.setFeedback("", "success");
                    $scope.traces = result;
                    $scope.showUploadOptions = false;
                    $scope.traceUploadError = false;
                });
            });
        };

        $scope.deleteTraceSequenceFile = function (fileId) {
            let foundTrace;
            let foundIndex;

            for (let i = 0; i < $scope.traces.data.length; i++) {
                let trace = $scope.traces.data[i];
                if (trace.fileId === fileId && trace.fileId !== undefined) {
                    foundTrace = trace;
                    foundIndex = i;
                    break;
                }
            }

            if (foundTrace !== undefined) {
                Util.remove("rest/parts/" + entryId + "/traces/" + foundTrace.id, {}, function (result) {
                    $scope.traces.data.splice(foundIndex, 1);
                    $scope.entryStatistics.sequenceCount = $scope.traces.data.length;
                });
            }
        };

        $scope.downloadTraceFile = function (trace) {
            //$window.open("rest/file/trace/" + trace.fileId + "?sid=" + $cookieStore.get("sessionId"), "_self");
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
                        locations: feature.locations
                    };

                    features.push(featureObject);
                }
            }

            return features;
        };


        let alignmentTracks = function (alignedSequence, referenceSequence) {
//            function magicDownload(text, fileName) {
//                let blob = new Blob([text], {
//                    type: "text/csv;charset=utf8;"
//                });
//
//// create hidden link
//                let element = document.createElement("a");
//                document.body.appendChild(element);
//                element.setAttribute("href", window.URL.createObjectURL(blob));
//                element.setAttribute("download", fileName);
//                element.style.display = "";
//
//                element.click();
//
//                document.body.removeChild(element);
//            }


            let alignment = {
                id: "iceAlignment",
                pairwiseAlignments: []
            };

            // get reference sequences features
            let features = convertFeaturedDNASequence(referenceSequence);

            for (let i = 0; i < alignedSequence.length; i += 1) {
                if (!alignedSequence[i].traceSequenceAlignment)
                    continue;

                if (alignedSequence[i].traceSequenceAlignment.queryStart > alignedSequence[i].traceSequenceAlignment.queryEnd) {
                    console.log(alignedSequence[i].traceSequenceAlignment);
                    continue;
                }

                let pairwiseAlignment = [
                    // reference sequence
                    {
                        sequenceData: {
                            id: i + 1, // refSequence.identifier
                            name: referenceSequence.name,
                            sequence: referenceSequence.sequence, // raw sequence
                            features: features
                        },
                        alignmentData: {
                            id: i + 1,
                            sequence: alignedSequence[i].traceSequenceAlignment.queryAlignment,
                            matchStart: alignedSequence[i].traceSequenceAlignment.queryStart,
                            matchEnd: alignedSequence[i].traceSequenceAlignment.queryEnd
                        }
                    },
                    // alignment sequence
                    {
                        sequenceData: {
                            id: i + 1,
                            name: alignedSequence[i].filename,
                            sequence: alignedSequence[i].sequence     // raw sequence
                        },
                        alignmentData: {
                            id: i + 1,
                            sequence: alignedSequence[i].traceSequenceAlignment.subjectAlignment,
                            matchStart: alignedSequence[i].traceSequenceAlignment.subjectStart,
                            matchEnd: alignedSequence[i].traceSequenceAlignment.subjectEnd
                        }
                    }
                ];

                alignment.pairwiseAlignments.push(pairwiseAlignment);

                if (alignedSequence[i].traceSequenceAlignment.subjectStart > alignedSequence[i].traceSequenceAlignment.subjectEnd) {
                    pairwiseAlignment[0].sequenceData.features = undefined;
                    magicDownload(JSON.stringify(pairwiseAlignment), 'alignedSequence.json');
                }
            }

            return alignment;
        };

        $scope.fetchSequenceTraces = function () {
            Util.get("rest/parts/" + entryId + "/traces", function (result) {
                if (result && result.data) {
                    Util.get("rest/parts/" + entryId + "/sequence", function (sequenceData) {
                        $scope.loadSequenceChecker(alignmentTracks(result.data, sequenceData));
                    })
                }
            })
        };

        $scope.loadSequenceChecker = function (alignments) {
            $scope.checkerEditor = $window.createAlignmentView(document.getElementById("sequence-checker-root"),
                alignments);
        }
    });
