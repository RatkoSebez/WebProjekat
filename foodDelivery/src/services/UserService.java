package services;

//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
//import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import beans.Comment;
import beans.Constants;
import beans.CustomerType;
import beans.Item;
import beans.Order;
import beans.Order.Status;
//import beans.Item;
//import beans.Item.Type;
import beans.Restaurant;
import beans.ShoppingCart;
import beans.User;
import beans.User.Role;
import repository.FileComments;
import repository.FileItems;
import repository.FileRestaurant;
import repository.FileUsers;


@Path("/userService")
public class UserService {
	
	@Context
	private ServletContext ctx;
	private ObjectMapper mapper = new ObjectMapper();
	
	public UserService() {}
	
	@PostConstruct
	public void init() {
		//System.out.println(ctx.getRealPath(""));
		String contextPath = ctx.getRealPath("");
		FileUsers fileUsers = FileUsers.getInstance(contextPath + "/data/users.json");
		FileRestaurant fileRestaurant = FileRestaurant.getInstance(contextPath + "/data/restaurants.json");
		FileItems fileItems = FileItems.getInstance(contextPath + "/data/items.json");
		FileComments fileComments = FileComments.getInstance(contextPath + "/data/comments.json");
		
		if(ctx.getAttribute("deliverers") == null) {
			HashMap<Long, String> deliverers = new HashMap<Long, String>();
			ctx.setAttribute("deliverers", deliverers);
		}
		if(ctx.getAttribute("users") == null) {
			List<User> users = fileUsers.getUsers();
			ctx.setAttribute("users", users);
		}
		if(ctx.getAttribute("restaurants") == null) {
			List<Restaurant> restaurants = fileRestaurant.getRestaurants();
			ctx.setAttribute("restaurants", restaurants);
		}
		if(ctx.getAttribute("fileUsers") == null) {
			ctx.setAttribute("fileUsers", fileUsers);
		}
		if(ctx.getAttribute("fileRestaurant") == null) {
			ctx.setAttribute("fileRestaurant", fileRestaurant);
		}
		if(ctx.getAttribute("fileItems") == null) {
			ctx.setAttribute("fileItems", fileItems);
		}
		if(ctx.getAttribute("fileComments") == null) {
			ctx.setAttribute("fileComments", fileComments);
		}
	}
	
	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean login(@Context HttpServletRequest request, User user) {
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User newUser = fileUsers.getUserForLogin(user.getUsername(), user.getPassword());
		if(newUser == null) {
			return false;
		}
		else {
			if(newUser.getShoppingCart() == null && newUser.getRole() == Role.Customer) {
				newUser.setShoppingCart(new ShoppingCart(new HashMap<String, Integer>(), newUser.getUsername(), 0));
				fileUsers.saveUser(newUser);
			}
			if(newUser.isBanned()) return false;
			request.getSession().setAttribute("user", newUser);
			return true;
		}
	}
	
	@POST
	@Path("/checkPassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean checkPassword(@Context HttpServletRequest request, User user) {
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		//System.out.println(fileUsers.checkPassword(user.getUsername(), user.getPassword()));
		return fileUsers.checkPassword(user.getUsername(), user.getPassword());
	}
	
	@GET
	@Path("/testLogin")
	@Produces(MediaType.TEXT_PLAIN)
	public String testLogin(@Context HttpServletRequest request) {
		User user;
		user = (User)request.getSession().getAttribute("user");
		if(user == null) return "not logged in";
		return user.getUsername() + ", " + user.getPassword();
	}
	
	@GET
	@Path("/logout")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean logout(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
		User user = null;
		user = (User) request.getSession().getAttribute("user");
		if (user != null) request.getSession().invalidate();
		response.sendRedirect("../../index.html");
		return true;
	}
	
	@POST
	@Path("/register")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean register(@Context HttpServletRequest request, User user) {
		//User user2 = mapper.readValue(request.getReader(), User.class);
		if(user.getRole() == null) user.setRole(Role.Customer);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		//da li username vec postoji
		//System.out.println(fileUsers.usernameExists(user.getUsername()));
		if(fileUsers.usernameExists(user.getUsername())) return false;
		//@SuppressWarnings("unchecked")
		ArrayList<User> users = fileUsers.getUsers();
		users.add(user);
		fileUsers.write();
		//uloguj korisnika kad se registruje
		if(user.getRole() == Role.Customer) request.getSession().setAttribute("user", user);
		return true;
	}
	
	@POST
	@Path("/editUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean editUser(@Context HttpServletRequest request, User user) {
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		ArrayList<User> users = fileUsers.getUsers();
		for(int i=0; i<users.size(); i++) {
			//System.out.println(users.get(i).getUsername() + ", " + user.getUsername());
			if(users.get(i).getUsername().equals(user.getUsername())) {
				users.get(i).setName(user.getName());
				users.get(i).setSurname(user.getSurname());
				users.get(i).setBirthDate(user.getBirthDate());
				if(!user.getPassword().equals("")) users.get(i).setPassword(user.getPassword());
			}
		}
		fileUsers.write();
		return true;
	}
	
	@GET
	@Path("/getUsers")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getUsers(@Context HttpServletRequest request) throws JsonProcessingException {
		User user = (User) request.getSession().getAttribute("user");
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		fileUsers.userIsSuspicious();
		ArrayList<User> users = new ArrayList<User>();
		if(user.getRole() == Role.Admin) {
			users = fileUsers.getUsers();
		}
		else if(user.getRole() == Role.Manager) {
			users = fileUsers.getUsersForManager(user.getRestaurant().getName());
		}
		String usersJson = mapper.writeValueAsString(users);
		return usersJson;
	}
	
	@GET
	@Path("/getLoggedInUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getLoggedInUser(@Context HttpServletRequest request) {
		User user = (User) request.getSession().getAttribute("user");
		try {
			String userJson = mapper.writeValueAsString(user);
			return userJson;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@GET
	@Path("/getRestaurant")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getRestaurant(@QueryParam("name") String name) {
		//System.out.println(name + "yo");
		FileRestaurant fileRestaurant = (FileRestaurant) ctx.getAttribute("fileRestaurant");
		Restaurant restaurant = fileRestaurant.getRestaurant(name);
		try {
			String usersJson = mapper.writeValueAsString(restaurant);
			return usersJson;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@GET
	@Path("/getRestaurants")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getRestaurants() {
		@SuppressWarnings("unchecked")
		ArrayList<Restaurant> restaurants = (ArrayList<Restaurant>) ctx.getAttribute("restaurants");
		try {
			String usersJson = mapper.writeValueAsString(restaurants);
			return usersJson;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@POST
	@Path("/newRestaurant")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean newRestaurant(@Context HttpServletRequest request, Restaurant restaurant) {
		FileRestaurant fileRestaurant = (FileRestaurant) ctx.getAttribute("fileRestaurant");
		ArrayList<Restaurant> restaurants = fileRestaurant.getRestaurants();
		restaurants.add(restaurant);
		fileRestaurant.write();
		//System.out.println(restaurants.size());
		//System.out.println(restaurants.size() + ", " + fileRestaurant.getRestaurants().size());
		return true;
	}
	
	@POST
	@Path("/saveImage")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean saveImage(String data) throws IOException {
		//pre sam hteo da slike cuvam u falju ali nema potrebe jer se one nalaze u objektu restorana
		FileRestaurant fileRestaurant = (FileRestaurant) ctx.getAttribute("fileRestaurant");
		//System.out.println("ovde sam");
		//System.out.println(base64Image.charAt(20));
		//String base64Image = data.split(",")[1];
		//byte[] imageBytes = DatatypeConverter.parseBase64Binary(base64Image);
		//BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
		Restaurant restaurant = fileRestaurant.getLastRestaurant();
		//File outputfile = new File(ctx.getRealPath("") + "/images/" + restaurant.getName() + ".png");
		restaurant.setImage(data);
		fileRestaurant.write();
		//ImageIO.write(image, "png", outputfile);
		return true;
	}
	
	@GET
	@Path("/getFreeManagers")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getFreeManagers() throws JsonProcessingException {
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		ArrayList<User> managers = fileUsers.getFreeManagers();
		String usersJson = mapper.writeValueAsString(managers);
		//System.out.println(managers.size());
		return usersJson;
	}
	
	@POST
	@Path("/setManagersRestaurant")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean setManagersRestaurant(String username) throws IOException {
		if(username.equals("no free managers")) return false;
		FileRestaurant fileRestaurant = (FileRestaurant) ctx.getAttribute("fileRestaurant");
		Restaurant restaurant = fileRestaurant.getLastRestaurant();
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User manager = fileUsers.getUser(username);
		manager.setRestaurant(restaurant);
		fileUsers.write();
		//System.out.println(username + "yo");
		return true;
	}
	
	@GET
	@Path("/getItems")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getItems(@QueryParam("name") String name) throws JsonProcessingException {
		//System.out.println("ime: " + name);
		FileItems fileItems = (FileItems) ctx.getAttribute("fileItems");
		ArrayList<Item> items = fileItems.getItems(name);
		String usersJson = mapper.writeValueAsString(items);
		//System.out.println(managers.size());
		return usersJson;
	}
	
	@GET
	@Path("/getItem")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getItem(@QueryParam("name") String name) throws JsonProcessingException {
		//System.out.println("ime: " + name);
		FileItems fileItems = (FileItems) ctx.getAttribute("fileItems");
		Item item = fileItems.getItem(name);
		String usersJson = mapper.writeValueAsString(item);
		//System.out.println(managers.size());
		return usersJson;
	}
	
	@POST
	@Path("/saveItemImage")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean saveItemImage(String data) throws IOException {
		FileItems fileItems = (FileItems) ctx.getAttribute("fileItems");
		Item item = fileItems.getLastItem();
		//File outputfile = new File(ctx.getRealPath("") + "/images/" + restaurant.getName() + ".png");
		item.setImage(data);
		fileItems.write();
		//ImageIO.write(image, "png", outputfile);
		return true;
	}
	
	@POST
	@Path("/newItem")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean newItem(Item item) {
		FileItems fileItems = (FileItems) ctx.getAttribute("fileItems");
		ArrayList<Item> items = fileItems.getItems();
		if(fileItems.itemNameEqists(item.getName())) return false;
		items.add(item);
		fileItems.write();
		return true;
	}
	
	@GET
	@Path("/addItemToCart")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean addItemToCart(@QueryParam("name") String name, @QueryParam("quantity") int quantity, @QueryParam("flag") String flag, @Context HttpServletRequest request) {
		//ako je flag yes to znaci da je funkcija pozvana iz shopping carta i onda samo treba da azuriram vrednost quantity
		//ako je flag no ona je pozvana is restaurant.js i onda dodajem quantity na njegovu staru vrednost
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User user = (User) request.getSession().getAttribute("user");
		Map<String, Integer> mp = user.getShoppingCart().getItemAndQuantity();
		if(mp.size() == 0) mp = new HashMap<String, Integer>();
		//System.out.println(flag);
		if(mp.containsKey(name) == false) {
			mp.put(name, quantity);
		}
		else {
			if(flag.equals("yes")) mp.put(name, quantity);
			else mp.put(name, mp.get(name) + quantity);
		}
		user.getShoppingCart().setItemAndQuantity(mp);
		fileUsers.saveUser(user);
		//System.out.println(name + ", " + quantity);
		return true;
	}
	
	@GET
	@Path("/getUserItems")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getUserItems(@Context HttpServletRequest request) throws JsonProcessingException {
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User user = (User)request.getSession().getAttribute("user");
		Map<String, Integer> mp = fileUsers.getUserItems(user.getUsername());
		//System.out.println(mp.size());
		String usersJson = mapper.writeValueAsString(mp);
		//System.out.println(usersJson);
		return usersJson;
	}
	
	@GET
	@Path("/removeItemFromCart")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean removeItemFromCart(@QueryParam("name") String name, @Context HttpServletRequest request) {
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User user = (User) request.getSession().getAttribute("user");
		Map<String, Integer> mp = user.getShoppingCart().getItemAndQuantity();
		//System.out.println(user.getShoppingCart().getItemAndQuantity().size());
		mp.remove(name);
		//System.out.println(user.getShoppingCart().getItemAndQuantity().size());
		fileUsers.saveUser(user);
		//System.out.println(name + ", " + quantity);
		return true;
	}
	
	@POST
	@Path("/getItemsForShoppingCart")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getItemsForShoppingCart(String data, @Context HttpServletRequest request) throws IOException {
		data = data.substring(10);
		data = data.substring(0, data.length() - 2);
		data = data.replace("\"", "");
		String [] names = data.split(",");
		User user = (User) request.getSession().getAttribute("user");
		FileItems fileItems = (FileItems) ctx.getAttribute("fileItems");
		ArrayList<Item> items = new ArrayList<Item>();
		for(int i=0; i<names.length; i++) {
			items.add(fileItems.getItem(names[i]));
		}
		/*@SuppressWarnings("unchecked")
		ArrayList<Item> items2 = (ArrayList<Item>) items.clone();
		if(user.getCustomerType() != null && items2.get(0) != null) {
			double discount = user.getCustomerType().getDiscount();
			for(int i=0; i<items2.size(); i++) {
				items2.get(i).setPrice(items2.get(i).getPrice()*(100-discount)/100);
			}
		}*/
		String usersJson = mapper.writeValueAsString(items);
		//System.out.println(data);
		return usersJson;
	}
	
	@POST
	@Path("/createOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean createOrder(Order order, @Context HttpServletRequest request) throws IOException {
		order.setResrourant(order.getItems().get(0).getRestaurant());
		order.setStatus(Status.Processing);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User user = (User)request.getSession().getAttribute("user");
		//praznim korpu korisnika
		Map<String, Integer> mp = user.getShoppingCart().getItemAndQuantity();
		mp = Collections.<String, Integer>emptyMap();
		user.getShoppingCart().setItemAndQuantity(mp);
		//ukupna_cena_porudžbine/1000 * 133
		int points = (int) Math.round((order.getPrice()/10)*133);
		int currentPoints = user.getDiscountPoints();
		user.setDiscountPoints(currentPoints + points);
		order.setCustomersName(user.getName());
		order.setCustomersSurname(user.getSurname());
		order.setOrderId(fileUsers.generateOrdersId());
		ArrayList<Order> orders = user.getCustomersOrders();
		if(orders == null) orders = new ArrayList<Order>();
		orders.add(order);
		user.setCustomersOrders(orders);
		CustomerType type = null;
		if(currentPoints + points >= Constants.GOLD_CUSTOMER_POINTS) type = new CustomerType(2);
		else if(currentPoints + points >= Constants.SILVER_CUSTOMER_POINTS) type = new CustomerType(1);
		else if(currentPoints + points >= Constants.BRONZE_CUSTOMER_POINTS) type = new CustomerType(0);
		user.setCustomerType(type);
		fileUsers.write();
		//System.out.println(order.getResrourant());
		return true;
	}
	
	@GET
	@Path("/getOrders")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getOrders(@Context HttpServletRequest request) throws JsonProcessingException {
		//ovde moze if pa da u zavisnosti od uloge vratis one porudzbine koje treba da budu vidljive
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User user = (User)request.getSession().getAttribute("user");
		if(user.getRole() == Role.Customer) {
			ArrayList<Order> allOrders = user.getCustomersOrders();
			ArrayList<Order> orders = new ArrayList<Order>();
			if(allOrders != null) {
				for(int i=0; i<allOrders.size(); i++) {
					if(allOrders.get(i).getStatus() != Status.Delivered) orders.add(allOrders.get(i));
				}
			}
			String usersJson = mapper.writeValueAsString(orders);
			return usersJson;
		}
		else if(user.getRole() == Role.Deliverer) {
			ArrayList<Order> orders = fileUsers.getDelivererOrders(user.getUsername());
			String usersJson = mapper.writeValueAsString(orders);
			return usersJson;
		}
		else if(user.getRole() == Role.Manager) {
			if(user.getRestaurant() == null) return null;
			ArrayList<Order> orders = fileUsers.getManagerOrders(user.getRestaurant().getName());
			String usersJson = mapper.writeValueAsString(orders);
			return usersJson;
		}
		return null;
	}
	
	@GET
	@Path("/getRestaurantType")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getRestaurantType(@QueryParam("restaurant") String restaurant) throws JsonProcessingException {
		FileRestaurant fileRestaurant = (FileRestaurant) ctx.getAttribute("fileRestaurant");
		//System.out.println(fileRestaurant.getRestaurantType(restaurant));
		String type = fileRestaurant.getRestaurantType(restaurant);
		//System.out.println(type);
		String usersJson = mapper.writeValueAsString(type);
		return usersJson;
	}
	
	@POST
	@Path("/cancelOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean cancelOrder(String data, @Context HttpServletRequest request) {
		//System.out.println(data);
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		String tokenPrice[] = tokens[1].split(":");
		long id = Integer.parseInt(tokenId[1]);
		int price = Integer.parseInt(tokenPrice[1]);
		//System.out.println(id + ", " + price);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		User user = (User)request.getSession().getAttribute("user");
		fileUsers.cancelOrder(user.getUsername(), id);
		int newDiscountPoints = (int) Math.max(user.getDiscountPoints() - (price/10.0)*133*4, 0);
		user.setDiscountPoints(newDiscountPoints);
		CustomerType type = null;
		if(newDiscountPoints >= Constants.GOLD_CUSTOMER_POINTS) type = new CustomerType(2);
		else if(newDiscountPoints >= Constants.SILVER_CUSTOMER_POINTS) type = new CustomerType(1);
		else if(newDiscountPoints >= Constants.BRONZE_CUSTOMER_POINTS) type = new CustomerType(0);
		user.setCustomerType(type);
		fileUsers.write();
		return true;
	}
	
	@POST
	@Path("/processedOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean processedOrder(String data, @Context HttpServletRequest request) {
		//System.out.println(data);
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		long id = Integer.parseInt(tokenId[1]);
		//System.out.println(id + ", " + price);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		//User user = (User)request.getSession().getAttribute("user");
		fileUsers.processOrder(id);
		fileUsers.write();
		//System.out.println(id);
		return true;
	}
	
	@POST
	@Path("/prepareOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean prepareOrder(String data, @Context HttpServletRequest request) {
		//System.out.println(data);
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		long id = Integer.parseInt(tokenId[1]);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		fileUsers.prepareOrder(id);
		fileUsers.write();
		return true;
	}
	
	@POST
	@Path("/waitingForDeliveryMan")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean waitingForDeliveryMan(String data, @Context HttpServletRequest request) {
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		String username[] = tokens[1].split(":");
		long id = Integer.parseInt(tokenId[1]);
		@SuppressWarnings("unchecked")
		HashMap<Long, String> deliverers = (HashMap<Long, String>) ctx.getAttribute("deliverers");
		deliverers.put(id, username[1]);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		fileUsers.waitingForDeliveryMan(id);
		fileUsers.write();
		return true;
	}
	
	@POST
	@Path("/waitingForApproval")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean waitingForApproval(String data, @Context HttpServletRequest request) {
		//System.out.println(data);
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		long id = Integer.parseInt(tokenId[1]);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		@SuppressWarnings("unchecked")
		HashMap<Long, String> deliverers = (HashMap<Long, String>) ctx.getAttribute("deliverers");
		fileUsers.waitingForApproval(id, deliverers.get(id));
		//System.out.println(deliverers.get(id));
		fileUsers.write();
		return true;
	}
	
	@POST
	@Path("/inTransportOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean inTransportOrder(String data, @Context HttpServletRequest request) {
		//System.out.println(data);
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		long id = Integer.parseInt(tokenId[1]);
		//System.out.println(id + ", " + price);
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		//User user = (User)request.getSession().getAttribute("user");
		fileUsers.inTransportOrder(id);
		fileUsers.write();
		//System.out.println(id);
		return true;
	}
	
	@GET
	@Path("/getRestaurantsForComments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getRestaurantsForComments(@QueryParam("username") String username) throws JsonProcessingException {
		//prolazim kroz ordere kupca i koji je order delivered taj restoran mogu da komentarisem
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		//dobijam imena restorana koji mogu komentarisati
		ArrayList<String> restaurants = fileUsers.getRestaurantsForComment(username);
		String usersJson = mapper.writeValueAsString(restaurants);
		return usersJson;
	}
	
	@POST
	@Path("/newComment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean newComment(Comment comment, @Context HttpServletRequest request) {
		User user = (User)request.getSession().getAttribute("user");
		comment.setUser(user.getUsername());
		FileComments fileComments = (FileComments) ctx.getAttribute("fileComments");
		comment.setId(fileComments.generateCommentsId());
		ArrayList<Comment> comments = fileComments.getComments();
		comments.add(comment);
		fileComments.write();
		//System.out.println(comment.getComment());
		return true;
	}
	
	@GET
	@Path("/getCommentsForRestaurant")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getCommentsForRestaurant(@QueryParam("name") String name) throws JsonProcessingException {
		FileComments fileComments = (FileComments) ctx.getAttribute("fileComments");
		ArrayList<Comment> comments = fileComments.getCommentsForRestaurant(name);
		//System.out.println(comments.size());
		String usersJson = mapper.writeValueAsString(comments);
		return usersJson;
	}
	
	@GET
	@Path("/updateCommentApproval")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean updateCommentApproval(@QueryParam("id") long id, @QueryParam("approve") Boolean approve) throws JsonProcessingException {
		//System.out.println(id + ", " + approve);
		FileComments fileComments = (FileComments) ctx.getAttribute("fileComments");
		fileComments.updateCommentApproval(id, approve);
		fileComments.write();
		return true;
	}
	
	@GET
	@Path("/getComments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getComments(@QueryParam("restaurant") String restaurant, @Context HttpServletRequest request) throws JsonProcessingException {
		User user = (User)request.getSession().getAttribute("user");
		FileComments fileComments = (FileComments) ctx.getAttribute("fileComments");
		ArrayList<Comment> comments;
		if(user == null) comments = fileComments.getCommentsForRestaurant(restaurant, null);
		else comments = fileComments.getCommentsForRestaurant(restaurant, user.getRole());
		//System.out.println(comments.size());
		String usersJson = mapper.writeValueAsString(comments);
		return usersJson;
	}
	
	@GET
	@Path("/getAverageRating")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getAverageRating(@QueryParam("restaurant") String restaurant) throws JsonProcessingException {
		FileComments fileComments = (FileComments) ctx.getAttribute("fileComments");
		double averageRating = fileComments.getAverageRating(restaurant);
		String usersJson = mapper.writeValueAsString(averageRating);
		return usersJson;
	}
	
	@POST
	@Path("/deleteUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean deleteUser(String data, @Context HttpServletRequest request) {
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		String username = tokenId[1];
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		fileUsers.deleteUser(username);
		fileUsers.write();
		return true;
	}
	
	@POST
	@Path("/banUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean banUser(String data, @Context HttpServletRequest request) {
		data = data.substring(1, data.length() - 1);
		data = data.replace("\"", "");
		String tokens[] = data.split(",");
		String tokenId[] = tokens[0].split(":");
		String username = tokenId[1];
		FileUsers fileUsers = (FileUsers) ctx.getAttribute("fileUsers");
		fileUsers.banUser(username);
		fileUsers.write();
		return true;
	}
}
