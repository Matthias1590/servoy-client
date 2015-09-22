/* jasmine specs for Tabpanels go here */

describe('servoydefaultTabpanel component', function() {
    var $scope 
    var $compile  
	var $httpBackend 
	var $timeout
	var handlersMock = {			
			tabs: {
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
				}
			}
	}
	var modelMock = {
			tabs: {
				borderType: {
					borderStyle: {
						borderColor: "null null null null",
						borderStyle: "groove",
						borderWidth: "2px"
					},
					type: "EtchedBorder"					
				},
				fontType: undefined,
				tabs: [{						
						active: true,
						containsFormId: "solutions/dummyUnitTestSol/forms/tab1.html",
						foreground: null,
						name: null,
						relationName: null,
						text: "tab1"
					   },{
						active: false,
						containsFormId: "solutions/dummyUnitTestSol/forms/tab2.html",
						foreground: null,
						name: null,
						relationName: null,
						text: "tab2",
					   }
				],
				transparent: true,
				location: {
					x: 0,
					y:0
				},
				size: {
					width: 640,
					height: 480
				},
				name: 'tabs',
				enabled: true
					
			}
	}
    var apiMock ={
    	tabs:{
    		
    	}
    }
	
	beforeEach(function(){		
	  module('servoy-components')  // generated by ngHtml2JsPreprocessor from all .html template files , as strings in the svyTemplate module
	   // 1. Include your application module for testing.
      module('servoydefaultTabpanel');
	  
      // 2. Define a new mock module. (don't need to mock the servoy module for tabpanel since it receives it's dependencies with attributes in the isolated scope)
      // 3. Define a provider with the same name as the one you want to mock (in our case we want to mock 'servoy' dependency.
//      angular.module('servoyMock', [])
//          .factory('$X', function(){
//              // Define you mock behaviour here.
//          });

      // 4. Include your new mock module - this will override the providers from your original module.
//      angular.mock.module('servoyMock');

      // 5. Get an instance of the provider you want to test.
      inject(function(_$httpBackend_,_$rootScope_,_$compile_ ,$templateCache,_$q_,_$timeout_){
    	  // Set up the mock http service responses
    	  $httpBackend = _$httpBackend_;
    	  $httpBackend.when('GET', 'solutions/dummyUnitTestSol/forms/tab1.html').respond("<div>tab1 content</div>");
    	  $httpBackend.when('GET', 'solutions/dummyUnitTestSol/forms/tab2.html').respond("<div>tab2 content</div>");
    	  $httpBackend.when('GET', 'solutions/dummyUnitTestSol/forms/tab3.html').respond("<div>tab3 content</div>");
    	  $httpBackend.when('GET', 'solutions/dummyUnitTestSol/forms/tab4.html').respond("<div>tab4 content</div>");
    	  
    	  $compile = _$compile_
    	  $timeout = _$timeout_
    	  $scope = _$rootScope_.$new();
    	  $scope.handlers = angular.copy(handlersMock);
    	  $scope.model= angular.copy(modelMock); 
    	  $scope.api= angular.copy(apiMock); 
    	  
    	  function dellayedOkPromise(formname, visibility,relationname) {
    		  var defered = _$q_.defer()    		  
    		  $timeout(function(){
    			  defered.resolve("Ok")
    		  }, 100)    		  
    		  return defered .promise
			}    	  
    	  	$scope.handlers.tabs.svy_servoyApi.formWillShow = dellayedOkPromise;    
			$scope.handlers.tabs.svy_servoyApi.hideForm = dellayedOkPromise; 
  	  })
	});
    
  	it("should corecly change tab when clicked on tab2", function() {

             // This will find your directive and run everything
             var tabPanelComponent = $compile('<data-servoydefault-tabpanel name="tabs" svy-model="model.tabs" svy-api="api.tabs" svy-handlers="handlers.tabs" svy-servoyApi="handlers.tabs.svy_servoyApi"/>')($scope);
             
             // Now run a $digest cycle to update your template with new data
             $scope.$digest();
             var tabsArr = tabPanelComponent.find('li').find('a');
             var scope = tabPanelComponent.isolateScope(); 
             tabsArr[1].click()
             //click triggers select which calls a setFormVisible mock with uses $timeout 
             //$timeout.flush() simulates the 100ms passing for the promise to be resolved
             $timeout.flush()
             expect(tabsArr[1].parentElement.classList.contains('active')).toBe(true)
             expect(tabsArr[0].parentElement.classList.contains('active')).toBe(false)
             expect(scope.model.tabs[1].active).toBe(true);      
             expect(scope.model.tabs[0].active).toBe(false);  
           // TODO verify why tabPanelComponent doesn't contain  'tab2 content'
           //expect(tabPanelComponent.html()).toContain("tab2 content");
	  });
  	// leave duplicated code for first commits for clarity
  	it("should corecly change tab when changed via scripting with tabIndex", function() {
        var tabPanelComponent = $compile('<data-servoydefault-tabpanel name="tabs" svy-model="model.tabs" svy-api="api.tabs" svy-handlers="handlers.tabs" svy-servoyApi="handlers.tabs.svy_servoyApi"/>')($scope);        
        $scope.$digest();
        var tabsArr = tabPanelComponent.find('li').find('a');
        var iscope = tabPanelComponent.isolateScope();     
        //change tabindex on form scope
        $scope.model.tabs.tabIndex = 2;
        $scope.$digest();
        $timeout.flush()
        expect(tabsArr[1].parentElement.classList.contains('active')).toBe(true)
        expect(tabsArr[0].parentElement.classList.contains('active')).toBe(false)
        expect(iscope.model.tabs[1].active).toBe(true);      
        expect(iscope.model.tabs[0].active).toBe(false)
        //expect(tabPanelComponent.html()).toContain("tab2 content");
  	});
  	
  	it("tab panel api test", function() {
        var tabPanelComponent = $compile('<data-servoydefault-tabpanel name="tabs" svy-model="model.tabs" svy-api="api.tabs" svy-handlers="handlers.tabs" svy-servoyApi="handlers.tabs.svy_servoyApi"/>')($scope);        
        $scope.$digest();
        var iscope = tabPanelComponent.isolateScope();
        expect(iscope.api.getMaxTabIndex()).toBe(2);
        iscope.api.addTab('tab4');
        iscope.api.addTab('tab3', 'vTab3', 'Third tab text', 'Third tab tooltip', null, '#ff0000', '#00ff00', null, 2);
        expect(iscope.api.getMaxTabIndex()).toBe(4);
        //change tabindex on form scope
        $scope.model.tabs.tabIndex = 3;
        $scope.$digest();
        $timeout.flush()
        expect(iscope.api.getSelectedTabFormName()).toBe('tab3');
        
        iscope.api.setTabFGColorAt(4, 'green');
        iscope.api.setTabTextAt(4, 'Fourth tab text');
        
        expect(iscope.api.getTabFGColorAt(3)).toBe('#ff0000');
        expect(iscope.api.getTabFGColorAt(4)).toBe('green');
        
        expect(iscope.api.getTabNameAt(3)).toBe('vTab3');
        
        expect(iscope.api.getTabTextAt(3)).toBe('Third tab text');
        expect(iscope.api.getTabTextAt(4)).toBe('Fourth tab text');
		
		iscope.api.setTabEnabledAt(3,true);
		iscope.api.setTabEnabledAt(4,false);
		expect(iscope.api.isTabEnabledAt(3)).toBe(true);
		expect(iscope.api.isTabEnabledAt(4)).toBe(false);
		
		iscope.api.setSize(200,300);
		expect(iscope.api.getWidth()).toBe(200);
		expect(iscope.api.getHeight()).toBe(300);
		
		iscope.api.setLocation(100,150)
		expect(iscope.api.getLocationX()).toBe(100)
		expect(iscope.api.getLocationY()).toBe(150)
		
		expect(iscope.api.getName()).toBe('tabs');
		
        iscope.api.removeTabAt(1);
        expect(iscope.api.getTabNameAt(2)).toBe('vTab3');
        iscope.api.removeTabAt(1);
        expect(iscope.api.getTabNameAt(1)).toBe('vTab3');
        
        expect($scope.model.tabs.tabIndex).toBe(1);
        
        iscope.api.removeAllTabs();
        expect(iscope.api.getMaxTabIndex()).toBe(0);
        
        expect($scope.model.tabs.tabIndex).toBe(-1);
		
		
  	});

}); 
