<?php
	if ( ! defined('WEDDING_API_DIR')) { exit; }
	
	class WeddingPlannerApi {

		$api_url = 'https://weddingplanner.appspot.com';
		
		public function __construct( $api_url = false ) {

			if( $api_url )
				$this->api_url = $api_url;

		}

		public function person($method = 'GET', $arguments = array()) {



		}

	}