angular.module('servoydefaultPassword',['servoy']).directive('servoydefaultPassword', function() {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        api: "=svyApi",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
          $scope.findMode = false;
          
          $scope.onClick = function(event){
        	  if ($scope.model.editable == false && $scope.handlers.onActionMethodID)
        	  {
        		  $scope.handlers.onActionMethodID(event);
        	  }	
          }
          
    	 // fill in the api defined in the spec file
    	 $scope.api.onDataChangeCallback = function(event, returnval) {
    		 if(!returnval) {
    			 $element[0].childNodes[0].focus();
    		 }
    	 },
    	 /**
    	 * Request the focus in this password field.
    	 * @example %%prefix%%%%elementName%%.requestFocus();
    	 */
    	 $scope.api.requestFocus = function() { 
    		  $element[0].childNodes[0].focus()
    	 }
    	 
      },
      templateUrl: 'servoydefault/password/password.html'
    };
  })

  
  
  
  
