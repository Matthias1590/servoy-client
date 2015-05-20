angular.module('servoydefaultSplitpane',['servoy']).directive('servoydefaultSplitpane', function($apifunctions) {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        svyServoyapi: "=",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  
    	  $scope.pane1MinSize = 30;
    	  $scope.pane2MinSize = 30;
    	  
    	  $scope.resizeWeight = 0;
    	  
    	  $scope.$watch("model.size.height", function(newValue, oldValue) {
    		 if($scope.model.tabOrientation == -3) {
    			 var delta = newValue - oldValue;
    			 if(delta != 0) {
    				 $scope.api.setDividerLocation(($scope.api.getDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight));
    			 }
    		 } 
    	  });
    	  $scope.$watch("model.size.width", function(newValue, oldValue) {
     		 if($scope.model.tabOrientation == -2) {
     			 var delta = newValue - oldValue;
     			 if(delta != 0) {
     				$scope.api.setDividerLocation(($scope.api.getDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight)); 
     			 }     				 
     		 }
     	  });
    	  
    	  if ($scope.model.tabs && $scope.model.tabs[0] && $scope.model.tabs[0].containsFormId) {
    		  $scope.svyServoyapi.formWillShow($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
    	  }
    	  if ($scope.model.tabs && $scope.model.tabs[1] && $scope.model.tabs[1].containsFormId) {
    		  $scope.svyServoyapi.formWillShow($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,1);
    	  }
    	  $scope.onChange = function() {
    		  if($scope.handlers.onChangeMethodID) $scope.handlers.onChangeMethodID(-1,$.Event("change"));
    	  }
    	  
          $scope.getForm = function(tab) {
        	  if (!tab) return null;
        	  return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
          }
          
          // the api defined in the spec file
          /**
           * Not applicable for the splitpane.
           */
          $scope.api.addTab = function(form, nameArg, tabText, tooltip, iconURL, fg, bg, relation, index) {
       	   return false;
          }
          
          /**
           * Not applicable for the splitpane.
           */
          $scope.api.removeTabAt = function(index) {
       	   return false;
          }
          
          /**
           * Not applicable for of the splitpane.
           */
          $scope.api.removeAllTabs = function() {
       	   return false;
          }
          
          /**
      	  * Returns the maximum tab index for a specified splitpane.
      	  * @example var max = %%prefix%%%%elementName%%.getMaxTabIndex();
      	  * @return {number} maximum tab index, 1 in case of the splitpane
      	  */
          $scope.api.getMaxTabIndex = function() {
       	   return 1; 
          }
           
          /**
      	  * Returns the foreground color for a specified tab of a splitpane. 
      	  * @example var color = %%prefix%%%%elementName%%.getTabFGColorAt(1); 
      	  * @param i the number of the specified tab
      	  * @return {String} color as hexadecimal RGB string
      	  */
          $scope.api.getTabFGColorAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.foreground  : '';
          }
          
          /**
      	  * Returns the form name for a specified tab of a splitpane.
      	  * @example var formName = %%prefix%%%%elementName%%.getTabFormNameAt(1);
      	  * @param i index of the tab
      	  * @return {String} the name of the form
      	  */
          $scope.api.getTabFormNameAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.containsFormId  : '';
          }

          /**
      	  * Returns the name - the "name" design time property value - for a specified tab of the splitpane. 
      	  * @example var tabName = %%prefix%%%%elementName%%.getTabNameAt(1);
      	  * @param i The number of the specified tab.
      	  * @return {String} The tab name
      	  */
          $scope.api.getTabNameAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.name  : '';
          }

          /**
     	  * Returns the relation name for a specified tab of the splitpane.
     	  * @example var relName = %%prefix%%%%elementName%%.getTabRelationNameAt(1);
     	  * @param i index of the tab
     	  * @return {String} relation name
     	  */
          $scope.api.getTabRelationNameAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.relationName  : '';
          }
          
          /**
      	  * Returns the text for a specified tab of a splitpane. 
      	  * @example var tabText = %%prefix%%%%elementName%%.getTabTextAt(1);
      	  * @param i The number of the specified tab.
      	  * @return {String} The tab text.
      	  */
          $scope.api.getTabTextAt = function(index) {
       	   return null;
          }
          
          /**
      	  * Returns the enabled status of a specified tab in a splitpane.
      	  * @example var status = %%prefix%%%%elementName%%.isTabEnabledAt(1); 
      	  * @param i the number of the specified tab
      	  * @return {Boolean} True if tab is enabled, false otherwise.
      	  */
          $scope.api.isTabEnabledAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? (tab.disabled == undefined ? true : !tab.disabled) : true;
          }

          /**
      	  * Sets the status of a specified tab in a splitpane.
      	  * @example %%prefix%%%%elementName%%.setTabEnabledAt(1,true);
       	  * @param i the number of the specified tab.
      	  * @param b true if enabled; or false if disabled.
      	  */
          $scope.api.setTabEnabledAt = function(index, enabled) {
       	   var tab = $scope.getTabAt(index);
       	   if(tab) {
       		   tab.disabled = !enabled;
       	   }
          }
          /**
           * Sets the foreground color for a specified tab in a splitpane.
      	   * @example %%prefix%%%%elementName%%.setTabFGColorAt(3,'#000000');
      	   * @param i the number of the specified tab
      	   * @param s the hexadecimal RGB color value to be set.
      	  */
          $scope.api.setTabFGColorAt = function(index, fgcolor) {
       	   var tab = $scope.getTabAt(index);
       	   if(tab) {
       		   tab.foreground = fgcolor;
       	   }    	   
          }

          /**
           * Not applicable for the splitpane.
           */
          $scope.api.setTabTextAt = function(index, text) {    	   
          }

          $scope.api.getHeight = function() {
       	   return $scope.model.size.height;
          }
          
          $scope.api.getLocationX = function() {
       	   return $scope.model.location.x;
          }
          
          $scope.api.getLocationY = function() {
       	   return $scope.model.location.y;
          }

          $scope.api.getWidth = function() {
       	   return $scope.model.size.width;
          }
          
          $scope.api.setLocation = function(x, y) {
       	   $scope.model.location.x = x;
       	   $scope.model.location.y = y;
          }
          
          $scope.api.setSize = function(width, height) {
       	   $scope.model.size.width = width;
       	   $scope.model.size.height = height;
          }
          
          $scope.api.getElementType= function() {
       	   return 'SPLITPANE';
          }
          
          $scope.api.getName = function() {
       	   return $scope.model.name;
          }
          
          /**
      	   * Set a relationless or related form as left panel.
           * @example %%prefix%%%%elementName%%.setLeftForm(forms.orders);
      	   * @param form the specified form or form name you wish to add as left panel
      	   * @return {Boolean} value indicating if tab was successfully added
      	  */
          $scope.api.setLeftForm = function(form, relation){
       	      $scope.model.tabs[0] = {
    			   name: null,
    			   containsFormId: form,
    			   text: null,
    			   relationName: relation,
    			   active: false,
    			   disabled: false,
    			   foreground: null };
       	      $scope.svyServoyapi.formWillShow($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
        	  return true;
          }
          
          /**
      	   * Set a relationless or related form as right panel.
           * @example %%prefix%%%%elementName%%.setRightForm(forms.orders);
      	   * @param form the specified form or form name you wish to add as right panel
      	   * @return {Boolean} value indicating if tab was successfully added
      	  */
          $scope.api.setRightForm = function(form, relation) {
       	      $scope.model.tabs[1] = {
       			   name: null,
       			   containsFormId: form,
       			   text: null,
       			   relationName: relation,
       			   active: false,
       			   disabled: false,
       			   foreground: null };        	  
       	      $scope.svyServoyapi.formWillShow($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,1);        	  
        	  return true;
          }
          
          /**
     	   * Returns the left form of the split pane.
     	   * @example var leftForm = %%prefix%%%%elementName%%.getLeftForm();
           * @return {FormScope} left form of the split pane
     	  */
          $scope.api.getLeftForm = function() {
        	  return $scope.model.tabs[0].containsFormId;
          }
          
          /**
     	   * Returns the right form of the split pane.
     	   * @example var rightForm = %%prefix%%%%elementName%%.getRightForm();
           * @return {FormScope} right form of the split pane
     	  */
          $scope.api.getRightForm = function() {
        	  return $scope.model.tabs[1].containsFormId;
          }
          
          /**
           * Gets or sets if the components should continuously be redrawn as the divider changes position.
           * @return {Boolean} always true in case of the splitpane
           */
          $scope.api.getContinuousLayout = function() {
        	  return true;
          }
          
          /**
           * Not applicable in case of the splitpane.
           */
          $scope.api.setContinuousLayout = function(b) {
          }
          
          /**
      	   * Gets the divider location.
      	   * If location is less then 1 then the location will be considered at (location * 100) percent of the split pane from left,
      	   * otherwise it will represent the pixels from left.
      	   * @example var dividerLocation = %%prefix%%%%elementName%%.dividerLocation
      	   * @return {Number}
      	  */
          $scope.api.getDividerLocation = function() {
  			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
 			 var dividerLocation;
			 if($scope.model.tabOrientation == -3) {
				 dividerLocation = dividerEl.css('top'); 
			 }
			 else {
				 dividerLocation = dividerEl.css('left'); 
			 }
			 
			 return dividerLocation ? dividerLocation.substring(0, dividerLocation.length - 2) : 0;
          }
          
          /**
      	   * Sets the divider location.
      	   * If location is less then 1 then the location will be considered at (location * 100) percent of the split pane from left,
      	   * otherwise it will represent the pixels from left.
      	   * @example %%prefix%%%%elementName%%.dividerLocation = 0.75;
      	  */
          $scope.api.setDividerLocation = function(location) {
        	 if(location >= 0 ) {
	  			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
	 			 var pane1 = angular.element($element[0].querySelector(".split-pane1"));
	 			 var pane2 = angular.element($element[0].querySelector(".split-pane2"));
	
	 			 var pos = location;
	 			 if($scope.model.tabOrientation == -3) { 
	 				if(pos < 1) {
	 					pos = $scope.model.size.height * pos;
	 				}
	 				dividerEl.css('top', pos + 'px');
	 	            pane1.css('height', pos + 'px');
	 	            pane2.css('top', pos + 'px');
	 			 }
	 			 else {
	 				if(pos < 1) {
	 					pos = $scope.model.size.width * pos;
	 				}
	 				dividerEl.css('left', pos + 'px');
	 	            pane1.css('width', pos + 'px');
	 	            pane2.css('left', pos + 'px');
	 			 }
	 			$scope.onChange();
        	 }
          }
          
          /**
      	  * Gets the divider size in pixels.
      	  * @example var dividerSize = %%prefix%%%%elementName%%.dividerSize
      	  * @return the size in pixels
      	  */
          $scope.api.getDividerSize = function() {
 			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
 			 var dividerSize;
			 if($scope.model.tabOrientation == -3) {
				 dividerSize = dividerEl.css('height'); 
			 }
			 else {
				 dividerSize = dividerEl.css('width'); 
			 }
			 
			 return dividerSize ? dividerSize.substring(0, dividerSize.length - 2) : 0;
          }
          
          /**
       	  * Sets divider size in pixels.
       	  * @example %%prefix%%%%elementName%%.dividerSize = 10;
       	  */
          $scope.api.setDividerSize = function(size) {
        	 if(size >= 0) {
	 			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
				 if($scope.model.tabOrientation == -3) {
					 dividerEl.css('height', size + 'px'); 
				 }
				 else {
					 dividerEl.css('width', size + 'px'); 
				 }
        	 }
          } 
          
          /**
          * Gets the resize weight, which specifies how to distribute extra space when the size of the split pane changes.
      	  * A value of 0, the default, indicates the right/bottom component gets all the extra space (the left/top component acts fixed),
      	  * where as a value of 1 specifies the left/top component gets all the extra space (the right/bottom component acts fixed).
      	  * Specifically, the left/top component gets (weight * diff) extra space and the right/bottom component gets (1 - weight) * diff extra space
      	  * @example var resizeWeight = %%prefix%%%%elementName%%.resizeWeight
      	  */
          $scope.api.getResizeWeight = function() {
        	  return $scope.resizeWeight;
          }
          
          /**
          * Sets the resize weight, which specifies how to distribute extra space when the size of the split pane changes.
       	  * A value of 0, the default, indicates the right/bottom component gets all the extra space (the left/top component acts fixed),
       	  * where as a value of 1 specifies the left/top component gets all the extra space (the right/bottom component acts fixed).
       	  * Specifically, the left/top component gets (weight * diff) extra space and the right/bottom component gets (1 - weight) * diff extra space
       	  * @example %%prefix%%%%elementName%%.resizeWeight = 10;
       	  */
          $scope.api.setResizeWeight = function(resizeW) {
        	  $scope.resizeWeight = resizeW;
          }  
          
          /**
      	  * Gets left form minimum size in pixels.
      	  * @example var left = %%prefix%%%%elementName%%.leftFormMinSize
      	  */
          $scope.api.getLeftFormMinSize = function() {
        	  return $scope.pane1MinSize;
          }
          
          /**
       	  * Sets left form minimum size in pixels.
       	  * @example %%prefix%%%%elementName%%.leftFormMinSize = 100;
       	  */
          $scope.api.setLeftFormMinSize = function(minSize) {
        	  $scope.pane1MinSize = minSize;
          } 
          
          /**
       	  * Gets right form minimum size in pixels.
       	  * @example var right = %%prefix%%%%elementName%%.rightFormMinSize
       	  */
          $scope.api.getRightFormMinSize = function() {
        	  return $scope.pane2MinSize;
          }
          
          /**
           * Sets right form minimum size in pixels.
           * @example %%prefix%%%%elementName%%.rightFormMinSize = 100;
          */
          $scope.api.setRightFormMinSize = function(minSize) {
        	  $scope.pane2MinSize = minSize;
          }
          
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);
      },
      templateUrl: 'servoydefault/splitpane/splitpane.html'
    };
  })