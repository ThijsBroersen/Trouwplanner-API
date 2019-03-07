$(document).ready(function () {
    var $tabs = $('#horizontalTab');
    $tabs.responsiveTabs({
        rotate: false,
        startCollapsed: 'accordion',
        collapsible: 'accordion',
        setHash: true,
        disabled: [3,4],
        click: function(e, tab) {
            $('.info').html('Tab <strong>' + tab.id + '</strong> clicked!');
        },
        activate: function(e, tab) {
            $('.info').html('Tab <strong>' + tab.id + '</strong> activated!');
        },
        activateState: function(e, state) {
            //console.log(state);
            $('.info').html('Switched from <strong>' + state.oldState + '</strong> state to <strong>' + state.newState + '</strong> state!');
        }
    });
});

$(function(){

	$(document).on('click', '.goto--page', function(e) {

		console.log('click');
		
		e.preventDefault();
		var $page_id 	= $(this).data('goto');
		var $validate 	= $(this).data('validate');
		gotoPageNumber($page_id, $validate, $('.section--page.active').data('page'));

	});

	$('#availability-calendar').datepicker({
         dateFormat: "yy-mm-dd",
         minDate: 0,
         onSelect: function (date) {
            
            console.log( date );

        }
    });

});

function gotoPageNumber(page_id, validate, previous_page) {
	
	// if( validate !== true || validatePage(previous_page) ) {
	
		$('.container--pages .section--page').removeClass('active');
		$('.container--pages .section--page[data-page="' + page_id + '"]').addClass('active');

	// }

}