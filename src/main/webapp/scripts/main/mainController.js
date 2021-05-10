'use strict';

angular.module('wors.search.controller', [])
    .controller('MainPageController', function ($rootScope, $state, $scope, $uibModal) {
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

        $scope.runSearch = function () {
            $state.go("search.results");
        };

        $scope.showAddRegistryForm = function () {
            $uibModal.open({
                templateUrl: "scripts/main/modal/add-registry.html",
                controller: function ($scope, $uibModalInstance, Util) {
                    $scope.newRegistry = {};

                    $scope.submitAddRegistryForm = function () {
                        Util.post("rest/search/registries", $scope.newRegistry, function () {
                            $uibModalInstance.close();
                        })
                    };

                    $scope.disableAddRegistrySubmitButton = function () {
                        return !$scope.newRegistry.userName || !$scope.newRegistry.name
                            || !$scope.newRegistry.url;
                    };
                },
                backdrop: 'static'
            });
        };
    });
