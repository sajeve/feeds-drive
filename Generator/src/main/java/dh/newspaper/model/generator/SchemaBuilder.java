package dh.newspaper.model.generator;

import de.greenrobot.daogenerator.*;

import java.io.File;

public class SchemaBuilder {
	public static void main(String[] args) throws Exception {
		System.out.println("Start generate..");

		Schema schema = new Schema(100, "dh.newspaper.model.generated");

		addPathToContent(schema);
		addArticle(schema);
		addSubscription(schema);

		System.out.println("Current path = "+ new File(".").getAbsolutePath());
		new DaoGenerator().generateAll(schema, "../Newspaper/src/main/java");

		System.out.println("Terminated OK");
	}

	private static void addArticle(Schema schema) {
		Entity article = schema.addEntity("Article");
		article.addIdProperty().autoincrement();

		article.addStringProperty("articleUrl").notNull().unique();
		article.addStringProperty("parentUrl");
		article.addStringProperty("imageUrl");
		article.addStringProperty("title").notNull();
		article.addStringProperty("author");
		article.addStringProperty("excerpt");
		article.addStringProperty("content");
		article.addStringProperty("checksum"); //checksum of title+content (uppercase + ignore white space)
		article.addStringProperty("language");
		article.addLongProperty("openedCount");
		article.addDateProperty("published");
		article.addDateProperty("publishedDateString");
		article.addDateProperty("archived");
		article.addDateProperty("lastOpened");
		article.addDateProperty("lastUpdated");

//		Entity articleCategory = schema.addEntity("ArticleCategory");
//		articleCategory.addIdProperty();
//		articleCategory.addLongProperty("categoryId");
//		Property articleId = articleCategory.addLongProperty("articleId").notNull().getProperty();
//		ToMany toMany = article.addToMany(articleCategory, articleId);
	}

	private static void addPathToContent(Schema schema) {
		Entity entity = schema.addEntity("PathToContent");
		entity.addIdProperty().autoincrement();
		entity.addStringProperty("urlPattern").notNull().unique();
		entity.addStringProperty("xpath");
		entity.addStringProperty("language");
		entity.addBooleanProperty("enable");
		entity.addDateProperty("lastUpdate");
	}

	private static void addSubscription(Schema schema) {
		Entity entity = schema.addEntity("Subscription");
		entity.addIdProperty().autoincrement();
		entity.addStringProperty("feedsUrl").notNull().unique();
		entity.addStringProperty("tags");
		entity.addStringProperty("description").notNull();
		entity.addStringProperty("language");
		entity.addBooleanProperty("enable");
		entity.addStringProperty("encoding");
		entity.addStringProperty("publishedDateString");
		entity.addDateProperty("lastUpdate");
	}
}
