'use strict';

// dependencies for the ice application
angular.module('ice.dependencies', ['ngRoute',
    'ngResource',
    'ngCookies',
    'worsApp.filters',
    'worsApp.directives',
    'wors.search.controller',
    'ice.entry.controller',
    'ui.router',
    'ice.search',
    'ice.entry',
    'ice.common.service'
]);