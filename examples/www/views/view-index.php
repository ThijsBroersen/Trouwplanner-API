<?php if ( ! defined('WEDDING_API_DIR')) { exit; } ?>

<?php WeddingPlannerView::get_header(); ?>

<header class="header--bar">
	<div class="container">
		<h1 class="header--title">Melding huwelijk of geregistreerd partnerschap</h1>
	</div>
</header>
<div class="container page--holder">
    <div class="container--pages">
            
        <!-- First page -->
        <section class="section--page active" data-page="1">

        	<div class="page-intro">
        		<p>Hieronder ziet u onze ambtenaren en trouwlocaties. Door te klikken op een naam uit de lijst kunt u meer informatie bekijken. Alles wat u hier kiest zal bij uw aanvraag al ingevuld zijn.</p>
				<p>U kunt maximaal 12 maanden in de toekomst plannen.</p>
        	</div>

        	<div id="horizontalTab">
				<ul>
					<li><a href="#locations-tab"><i class="fas fa-home"></i> Locatie</a></li>
					<li><a href="#functionary-tab"><i class="fas fa-user-tie"></i> Ambtenaar</a></li>
					<li><a href="#availability-tab"><i class="fas fa-calendar-alt"></i> Beschikbaarheid</a></li>
				</ul>

				<div id="locations-tab">
					
					<select name="prep_location">
						<?php foreach ($locations_list as $key => $value) { ?>
							<option value="<?php echo $key; ?>"><?php echo $value; ?></option>
						<?php } ?>
					</select>

				</div>

				<div id="functionary-tab">
					
					<select name="prep_functionary">
						<?php foreach ($functionaries_list as $key => $value) { ?>
							<option value="<?php echo $key; ?>"><?php echo $value; ?></option>
						<?php } ?>
					</select>
				</div>

				<div id="availability-tab">
					<div id="availability-calendar"></div>

					<div class="table-responsive wepl_table--container">
						<table class="table wepl_table">
							<tbody>
								<tr class="wepl_table--timeslots">
									<td></td>
									<td>
										<strong>09:00</strong>
										<strong>10:30</strong>
									</td>
									<td>
										<strong>10:30</strong>
										<strong>11:00</strong>
									</td>
									<td>
										<strong>11:00</strong>
										<strong>12:30</strong>
									</td>
								</tr>
								<tr class="wepl_table--head">
									<th>Locaties</th>
									<td colspan="3"></td>
								</tr>
								<tr class="wepl_table--options">
									<th>Locatie 1</th>
									<td></td>
									<td class="selected"></td>
									<td></td>
								</tr>
								<tr class="wepl_table--options">
									<th>Locatie 2</th>
									<td class="blocked"></td>
									<td class="selected--col"></td>
									<td></td>
								</tr>
								<tr class="wepl_table--options">
									<th>Locatie 3</th>
									<td></td>
									<td class="selected--col"></td>
									<td></td>
								</tr>
								<tr class="wepl_table--head">
									<th>Ambtenaren</th>
									<td colspan="3"></td>
								</tr>
								<tr class="wepl_table--options">
									<th>Ambtenaar 1</th>
									<td></td>
									<td class="selected--col"></td>
									<td></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>

			</div>
			
			<hr>
			<a href="#" class="btn btn-default btn-lg btn-block goto--page" data-goto="2"><i class="fas fa-play"></i> Start aanvraag</a>

        </section>

        <!-- Second page -->
        <section class="section--page" data-page="2">
        	Klaar om te starten met de aanvraag!
        </section>

    </div>
</div>

<?php WeddingPlannerView::get_footer(); ?>