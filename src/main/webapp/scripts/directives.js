'use strict';

var directives = angular.module('worsApp.directives', []);

directives.directive("searchBar", function () {
    return {
        restrict: "E",
        templateUrl: "scripts/search/search-bar.html",
        controller: "SearchBarController"
    }
});

directives.directive('focus', function ($timeout, $rootScope) {
    return {
        restrict: 'A',
        link: function ($scope, $element, attrs) {
            $element[0].focus();
        }
    }
});

directives.directive("iceEntryAttachment", function () {
    return {
        restrict: "E",
        templateUrl: "scripts/entry/entry-attachment.html"
    }
});

directives.directive('stopEvent', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            element.bind(attr.stopEvent, function (e) {
                e.stopPropagation();
            });
        }
    };
});

directives.directive('myCurrentTime', function ($interval, dateFilter) {

    function link(scope, element, attrs) {
        var format, timeoutId;

        function updateTime() {
            element.text(dateFilter(new Date(), format));
        }

        scope.$watch(attrs.myCurrentTime, function (value) {
            format = value;
            updateTime();
        });

        element.on('$destroy', function () {
            $interval.cancel(timeoutId);
        });

        timeoutId = $interval(function () {
            updateTime(); // update DOM
        }, 1000);
    }

    return {
        link: link
    };
});



