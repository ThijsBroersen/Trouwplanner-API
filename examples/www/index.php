<?php
		
	// include the init class
	include __DIR__ . '/classes/class-weddingplanner-init.php';

	// run the class
	$wedding_planner = new WeddingplannerInit();
	$wedding_planner->init_view();