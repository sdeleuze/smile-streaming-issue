import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.junit.Test;

public class StreamingTests {

	@Test
	public void jsonStreaming() throws IOException {
		stream(new ObjectMapper());

	}

	@Test
	public void smileStreaming() throws IOException {
		stream(new ObjectMapper(new SmileFactory()));

	}

	public void stream(ObjectMapper mapper) throws IOException {
		JsonFactory factory = mapper.getFactory();

		List<byte[]> data = new ArrayList<>();
		data.add(mapper.writeValueAsBytes(new Person("foo1")));
		data.add(mapper.writeValueAsBytes(new Person("foo2")));
		data.add(mapper.writeValueAsBytes(new Person("foo3")));

		Jackson2Tokenizer.tokenize(data, factory, true);
	}

	private static class Person {

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			return !(this.name != null ? !this.name.equals(person.name) : person.name != null);
		}

		@Override
		public int hashCode() {
			return this.name != null ? this.name.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Person{" +
					"name='" + name + '\'' +
					'}';
		}
	}
}
