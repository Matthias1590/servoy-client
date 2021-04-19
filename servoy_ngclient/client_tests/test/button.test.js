
describe('svyButton component', function() {
	//jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000;
	var $scope;
	var $compile; 
	var $httpBackend;
	var $timeout;
	var handlersMock = {			
			myButton: {
				svy_servoyApi: {
					formWillShow: function(formname,relationname,formIndex) {
					},
					hideForm: function(formname,relationname,formIndex) {
					},
					getFormUrl: function (formId) {
					},
					apply: function(propertyName)
					{
					}
				},
		}
	}
	var modelMock = {
			myButton: {
				enabled:true,
				text:"button",
				visible:true,
				location:{
					"x":89,
					"y":43
				},
				toolTipText:"tooltip text content",
				size:{
					"width":114,
					"height":40
				},
				name:"myButton"
			}	}
	var apiMock ={
			myButton:{

			}
	}


	beforeEach(function() {
        sessionStorage.removeItem('svy_session_lock');
        module('ngSanitize');
        module('webSocketModule');
		module('servoy-components');  // generated by ngHtml2JsPreprocessor from all .html template files , as strings in the svyTemplate module
		// 1. Include your application module for testing.
		module('servoydefaultButton');

		// 2. Define a new mock module. (don't need to mock the servoy module for tabpanel since it receives it's dependencies with attributes in the isolated scope)
		// 3. Define a provider with the same name as the one you want to mock (in our case we want to mock 'servoy' dependency.
//		angular.module('servoyMock', [])
//		.factory('$X', function(){
//		// Define you mock behaviour here.
//		});

		// 4. Include your new mock module - this will override the providers from your original module.
//		angular.mock.module('servoyMock');

		// 5. Get an instance of the provider you want to test.
		inject(function(_$httpBackend_,_$rootScope_,_$compile_ ,$templateCache,_$q_,_$timeout_) {
			$compile = _$compile_
			$timeout = _$timeout_
			$scope = _$rootScope_.$new();
			$scope.handlers = angular.copy(handlersMock);
			$scope.model= angular.copy(modelMock); 
			$scope.api= angular.copy(apiMock); 
		})
		// mock timout
		jasmine.clock().install();
	});
	afterEach(function() {
		jasmine.clock().uninstall();
	})

	it("should show tooltip when mouseover", function() {
		var template= '<data-servoydefault-button name="myButton" svy-model="model.myButton" svy-api="api.myButton" svy-handlers="handlers.myButton" '+
		' svy-servoyApi="handlers.myButton.svy_servoyApi"/>'
		// This will find your directive and run everything
		var buttonComponent = $compile(template)($scope);
		// Now run a $digest cycle to update your template with new data
		$scope.$digest();
		//console.log("test function------" + buttonComponent[0].firstElementChild.tagName);
		buttonComponent[0].firstElementChild.triggermouseover();
		jasmine.clock().tick(1000);
		var tooltip = document.getElementById('mktipmsg');
		expect(tooltip.style.display != 'none').toBe(true);
		expect(tooltip.innerHTML.indexOf("tooltip text content")!=-1).toBe(true);
		jasmine.clock().tick(5000);
		buttonComponent[0].firstElementChild.triggermouseout();
		expect(tooltip.style.display == 'none').toBe(true);
		// update tooltip
		$scope.model.myButton.toolTipText = 'UPDATED tooltip text content';
		$scope.$digest();
		$scope.model.myButton.$modelChangeNotifier('toolTipText',$scope.model.myButton.toolTipText);
		buttonComponent[0].firstElementChild.triggermouseover();
		jasmine.clock().tick(800);
		expect(tooltip.innerHTML.indexOf("UPDATED tooltip text content")!=-1).toBe(true);
	});

	it("should have onclick", function() {

		var template= '<data-servoydefault-button name="myButton" svy-model="model.myButton" svy-api="api.myButton" svy-handlers="handlers.myButton" '+
		'svy-servoyApi="handlers.myButton.svy_servoyApi"/>'
		var clicked = false;
		$scope.handlers.myButton.onActionMethodID = function(event) {
			clicked = true;
		}
		$scope.$digest();
		// This will find your directive and run everything
		var buttonComponent = $compile(template)($scope);             
		// Now run a $digest cycle to update your template with new data
		$scope.$digest();
		//console.log(buttonComponent[0]);
		//console.log(Object.keys(buttonComponent[0].firstElementChild));
		buttonComponent[0].firstElementChild.click();
		 $timeout.flush();
		expect( clicked).toBe(true);
	});

	it("should have double and right click", function() {
		var template= '<data-servoydefault-button name="myButton" svy-model="model.myButton" svy-api="api.myButton" svy-handlers="handlers.myButton" '+
		'svy-servoyApi="handlers.myButton.svy_servoyApi"/>'
		var double = false;
		$scope.handlers.myButton.onDoubleClickMethodID = function(event) {
			double = true;
		};
		var right = false; 
		$scope.handlers.myButton.onRightClickMethodID = function(event) {
			right = true;
		};
		$scope.$digest();
		// This will find your directive and run everything
		var buttonComponent = $compile(template)($scope);             
		// Now run a $digest cycle to update your template with new data
		$scope.$digest();
		//buttonComponent[0].firstElementChild.dblclick()
		angular.element(buttonComponent[0].firstElementChild).triggerHandler("dblclick")
		angular.element(buttonComponent[0].firstElementChild).triggerHandler("contextmenu")
		$timeout.flush();
		expect(double).toBe(true);
		expect(right).toBe(true);
	});
}); 
