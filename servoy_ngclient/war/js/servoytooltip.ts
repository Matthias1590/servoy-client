angular.module('servoytooltip',[]).factory("$svyTooltipUtils", function($window){
	
	var tooltipDiv;
	function getTooltipDiv() {
		if(!tooltipDiv) {
			tooltipDiv = $window.document.createElement('div');
			tooltipDiv.id = 'mktipmsg';
			tooltipDiv.className = 'mktipmsg tooltip-inner'; // tooltip-inner class is also used by ui-bootstrap-tpls-0.10.0
			$window.document.getElementsByTagName('body')[0].appendChild(tooltipDiv);			
		}
		return tooltipDiv;
	}
	
	var tipmousemouveEventX, tipmousemouveEventY, tipmousemouveEventIsPage;
	function tipmousemove(e)
	{
		if(e.pageX || e.pageY)
		{
			tipmousemouveEventIsPage = true;
			tipmousemouveEventX = e.pageX;
			tipmousemouveEventY = e.pageY;
		}
		else if(e.clientX || e.clientY)
		{
			tipmousemouveEventIsPage = false;
			tipmousemouveEventX = e.clientX;
			tipmousemouveEventY = e.clientY;
		}	
	}
	
	var tipInitialTimeout, tipTimeout;
	function adjustAndShowTooltip(dismissDelay)
	{
		var x = 0;
		var y = 0;
			
		if(tipmousemouveEventX || tipmousemouveEventY)
		{
			if(tipmousemouveEventIsPage)
			{
				x = tipmousemouveEventX;
				y = tipmousemouveEventY;
			}
			else
			{
				x = tipmousemouveEventX + $window.document.body.scrollLeft + $window.document.documentElement.scrollLeft;
				y = tipmousemouveEventY + $window.document.body.scrollTop + $window.document.documentElement.scrollTop;
			}
		}

		var wWidth = 0, wHeight = 0;
	  	if( typeof( $window.innerWidth ) == 'number' )
	  	{
	    	//Non-IE
	    	wWidth = $window.innerWidth;
	    	wHeight = $window.innerHeight;
	  	}
	  	else if( $window.document.documentElement && ( $window.document.documentElement.clientWidth || $window.document.documentElement.clientHeight ) )
	  	{
	    	//IE 6+ in 'standards compliant mode'
	    	wWidth = $window.document.documentElement.clientWidth;
	    	wHeight = $window.document.documentElement.clientHeight;
	  	}
	  	else if( $window.document.body && ( $window.document.body.clientWidth || $window.document.body.clientHeight ) )
	  	{
	    	//IE 4 compatible
	    	wWidth = $window.document.body.clientWidth;
	    	wHeight = $window.document.body.clientHeight;
	  	}

	  	var tDiv = getTooltipDiv();
		tDiv.style.left = x + 10  + "px";	
		tDiv.style.top = y + 10 + "px";		
		tDiv.style.display = "block";
		var tooltipOffsetWidth = x + 10 + tDiv.offsetWidth; 
		let positionClass = "tooltip-bottom-right";
        let toTheLeft = false;
        let toTheTop= false;
        if(wWidth < tooltipOffsetWidth)
        {
            toTheLeft = true;
            var newLeft = x - 10 - tDiv.offsetWidth;
            if(newLeft < 0)
            {
                newLeft = 0;
                tDiv.style.width = x - 10 + "px";
            }
            tDiv.style.left = newLeft  + "px";
        }
        var tooltipOffsetHeight = y + 10 + tDiv.offsetHeight
        if(wHeight < tooltipOffsetHeight)
        {
            toTheTop = true;
            var newTop = y - 10 - tDiv.offsetHeight;
            tDiv.style.top = newTop  + "px";
        }
        if (toTheLeft)  {
            positionClass = toTheTop?"tooltip-top-left":"tooltip-bottom-left";
        }
        else if (toTheTop) positionClass = "tooltip-top-right";
        tDiv.classList.add(positionClass);
        
        tipTimeout = setTimeout(function() { _hideTooltip(positionClass); }, dismissDelay);
    }
    
    function _hideTooltip(classToRemove) {
		if($window.removeEventListener)
		{
			$window.removeEventListener('mousemove', tipmousemove, false);
		}
		else
		{
			$window.detachEvent('mousemove', tipmousemove);
		}
		clearTimeout(tipInitialTimeout);
		clearTimeout(tipTimeout);
		
		var tDiv = getTooltipDiv();
		tDiv.style.display = "none";

        if (classToRemove) {
        	tDiv.classList.remove(classToRemove);
        }
	}
	
	return {
		showTooltip : function (event, message, initialDelay, dismissDelay) {
			var e = event;
			if(!e) e = $window.event;
			
			var targ;
			if(e.target) targ = e.target;
			else if (e.srcElement) targ = e.srcElement;
			if(targ.nodeType == 3) // defeat Safari bug
				targ = targ.parentNode;

			if(targ.tagName && targ.tagName.toLowerCase() == "option")	// stop tooltip if over option element
			{
				_hideTooltip( null );
				return;
			}

			var tDiv = getTooltipDiv();
			tDiv.innerHTML = message;
			tDiv.style.zIndex = 1600;
			tDiv.style.width = "";
			tDiv.style.overflow = "hidden";
			
			tipmousemove(e);

			if($window.addEventListener)
			{
				$window.document.addEventListener('mousemove', tipmousemove, false);
			}
			else
			{
				$window.document.attachEvent('mousemove', tipmousemove);
			}
			tipInitialTimeout = setTimeout(function() { adjustAndShowTooltip(dismissDelay); }, initialDelay);
		},
		
		hideTooltip: function() {
			_hideTooltip( null );
		}  
	}
})
.directive('svyTooltip', function ($svyTooltipUtils) {
	return {
		restrict: 'A',
        link: function (scope, element, attrs) {
        	var tooltip =  ''
		        element.bind('mouseover', function(event) {
		        	if(tooltip) $svyTooltipUtils.showTooltip(event, tooltip, 750, 5000);
		        });
		        element.bind('mouseout', function(event) {
		        	if(tooltip) $svyTooltipUtils.hideTooltip();
		        })
        	scope.$watch(attrs['svyTooltip'],function(newVal){
        		tooltip = <string>newVal;        		
        	})
        	
        }
	};
});