'use strict';

// Declare app level module which depends on filters, and services, etc
angular
    .module('worsApp', ['ice.dependencies'])

    .run(function ($route, $location, $rootScope) {
        // change path without re-loading
        var original = $location.path;
        $location.path = function (path, reload) {
            console.log("reloading");

            if (reload === false) {
                var lastRoute = $route.current;
                var un = $rootScope.$on('$locationChangeSuccess', function () {
                    $route.current = lastRoute;
                    un();
                });
            }
            return original.apply($location, [path]);
        };
    })

    // this is run first
    .config(function ($locationProvider, $stateProvider, $urlRouterProvider) {
        // remove '#' from the url ?
        $locationProvider.html5Mode(true);

        // default
        $urlRouterProvider.otherwise('/');

        // angular ui
        $stateProvider
            .state('main', {
                url: '/',
                templateUrl: 'scripts/main/main-partial.html',
                controller: 'MainPageController'
            })
            .state('search', {
                url: '/search',
                templateUrl: 'scripts/main/search.html',
                controller: 'SearchController'
            })
            .state("search.results", {
                url: '/results',
                templateUrl: 'scripts/search/search-results.html'
            })
            .state('search.entry', {
                url: '/entry/:id',
                templateUrl: 'scripts/entry/entry.html'
            })
            .state('search.entry.option', {
                url: '/:option'
            })
    });
