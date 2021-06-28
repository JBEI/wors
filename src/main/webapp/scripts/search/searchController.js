'use strict';

angular.module('ice.search.controller', [])
    .controller('SearchController', function ($scope, $rootScope, $http, $location, $state, $filter, Selection, Util) {

        $scope.hStepOptions = [15, 30, 50, 100];

        var runAdvancedSearch = function (filters) {
            $scope.loadingSearchResults = true;
            Util.post("rest/search", filters, function (result) {
                $rootScope.searchResults = result;
                $rootScope.search.parameters.available = result.resultCount;

                $scope.loadingSearchResults = false;
            }, {}, function () {
                $scope.loadingSearchResults = false;
                $rootScope.searchResults = undefined;
            });
        };

        // init
        if (!$rootScope.search) {
            $rootScope.search = {
                queryString: "",
                blastQuery: {
                    blastProgram: "",
                    sequence: ""
                },

                parameters: {
                    start: 0,
                    retrieveCount: 30,
                    currentPage: 1,
                    maxSize: 5
                }
            };
            $rootScope.searchResults = {results: []};
        } else {
            $rootScope.search.parameters.start = 0;

            if (!$rootScope.search.blastQuery.blastProgram)
                $rootScope.search.blastQuery.blastProgram = "BLAST_N";

            runAdvancedSearch($rootScope.search);
        }

        $scope.setSelected = function (index) {
            $rootScope.search.parameters.start = index;
        };

        $scope.selectAllClass = function () {
            if (Selection.allSelected()) // || $scope.folder.entries.length === Selection.getSelectedEntries().length)
                return 'fa-check-square-o';

            if (Selection.hasSelection())
                return 'fa-minus-square';
            return 'fa-square-o';
        };

        $scope.selectAllSearchResults = function () {
            if (Selection.allSelected()) {
                Selection.setTypeSelection('none');
                Selection.setSearch(undefined);
            }
            else {
                Selection.setTypeSelection('all');
                Selection.setSearch(query);
            }
        };

        $scope.hStepChanged = function () {
            $rootScope.search.parameters.start = 0;
            $rootScope.search.parameters.currentPage = 1;
            runAdvancedSearch($rootScope.search);
        };

        $scope.searchResultPageChanged = function () {
            $rootScope.search.parameters.start = (($rootScope.search.parameters.currentPage - 1) * $rootScope.search.parameters.retrieveCount) + 1;
            runAdvancedSearch($rootScope.search);
        };

        $scope.getType = function (relScore) {
            if (relScore === undefined)
                return 'info';

            if (relScore >= 70)
                return 'success';
            if (relScore >= 30 && relScore < 70)
                return 'warning';
            if (relScore < 30)
                return 'danger';
            return 'info';
        };

        $scope.tooltipDetails = function (entry) {
            $scope.currentTooltip = undefined;
            Util.get("rest/parts/" + entry.id + "/tooltip", function (result) {
                $scope.currentTooltip = result;
            });
        };

        $scope.pageCounts = function (currentPage, resultCount, maxPageCount) {
            if (!maxPageCount)
                maxPageCount = 30;
            var pageNum = ((currentPage - 1) * maxPageCount) + 1;

            // number on this page
            var pageCount = (currentPage * maxPageCount) > resultCount ? resultCount : (currentPage * maxPageCount);
            return $filter('number')(pageNum) + " - " + $filter('number')(pageCount) + " of " + $filter('number')(resultCount);
        };
    })
    .controller('SearchBarController', function ($scope, $rootScope, $http, $location, $state, Util) {
        $scope.advancedMenu = {
            isOpen: false
        };

        var runAdvancedSearch = function (filters) {
            $scope.loadingSearchResults = true;

            Util.post("rest/search", filters, function (result) {
                $rootScope.searchResults = result;
                $rootScope.search.parameters.available = result.resultCount;
                $scope.loadingSearchResults = false;
                $state.go("search.results");
            }, {}, function () {
                $scope.loadingSearchResults = false;
                $rootScope.searchResults = undefined;
            });
        };

        $scope.searchParts = function () {
            $rootScope.search.parameters.start = 0;
            if ($rootScope.search.parameters.retrieveCount < 15)
                $rootScope.search.parameters.retrieveCount = 30;

            runAdvancedSearch($rootScope.search);
            if ($state.current.name !== "search.results") {
                $state.go("search.results");
            }

            $scope.advancedMenu.isOpen = false;
        };

        $scope.toggleAdvancedMenuDropdown = function ($event) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.advancedMenu.isOpen = !$scope.advancedMenu.isOpen;
        };
    });
