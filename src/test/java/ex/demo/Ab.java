package ex.demo;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.customizer.Values;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class Ab {

    private static final String STATISTIC_UPDATE_LIST = "STATISTIC_UPDATE_LIST";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected WebApplicationContext context;


    protected FixtureMonkey fixtureMonkey = FixtureMonkeyConfig.fixtureMonkey;


    protected Store fixtureStore(Map<String, Object> params) {
        ArbitraryBuilder<Store> builder = fixtureMonkey.giveMeBuilder(Store.class);
        setBuilderParams(params, builder);
        return builder.sample();
    }

    protected Product fixtureProduct(Map<String, Object> params) {
        ArbitraryBuilder<Product> builder = fixtureMonkey.giveMeBuilder(Product.class);
        setBuilderParams(params, builder);
        builder.set("board", null); // fixtureBoard(Map.of("products", List.of(prodcut))) 쓰도록 강제
        return builder.sample();
    }

    protected BoardDetail fixtureBoardDetail(Map<String, Object> params) {
        ArbitraryBuilder<BoardDetail> builder = fixtureMonkey.giveMeBuilder(BoardDetail.class);
        setBuilderParams(params, builder);
        builder.set("board", null); // fixtureBoard(Map.of("boardDetails", List.of(boardDetail))) 쓰도록 강제
        return builder.sample();
    }


    /**
     * BoardStatistic의 값 제어 커스텀해서 사용하려면 아래와 같은 방법으로 BoardStatistic을 저장해야합니다.
     * BoardStatistic boardStatistic = fixtureRanking(Map.of("boardReviewCount", 2L));
     * Board board = fixtureBoard(Map.of("boardStatistic", boardStatistic));
     * boardRepository.save(board);
     */
    protected BoardStatistic fixtureRanking(Map<String, Object> params) {
        ArbitraryBuilder<BoardStatistic> builder = fixtureMonkey.giveMeBuilder(
                BoardStatistic.class);
        builder.set("id", null);
        setBuilderParams(params, builder);
        builder.set("board", null); // // 위 예시처럼 사용하게 강제

        return builder.sample();
    }

    protected Board fixtureBoard(Map<String, Object> params) {
        ArbitraryBuilder<Board> builder = fixtureMonkey.giveMeBuilder(Board.class);
        setBuilderParams(params, builder);
        Board sample = builder.sample();

//        if (!params.containsKey("store")) {
//            // store 선저장을 까먹은 분을 위해
//            storeRepository.save(sample.getStore());
//        }
        return sample;
    }

    private void setBuilderParams(Map<String, Object> params, ArbitraryBuilder builder) {
        for (Entry<String, Object> entry : params.entrySet()) {
            if (isBidirectional(entry.getValue())) {
                // 양방향 관계일 때 Vaues.just로 넘겨줘야 순환참조가 걸리지 않습니다.
                builder = builder.set(entry.getKey(), Values.just(entry.getValue()));
            } else {
                builder = builder.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean isBidirectional(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof List<?> list) {
            for (Object item : list) {
                if (isBidirectional(item)) {
                    return true;
                }
            }
            return false;
        }

        Class<?> objClass = obj.getClass();
        if (!isEntityClass(objClass)) {
            return false;
        }

        try {
            for (Field field : objClass.getDeclaredFields()) {
                // Skip final, static, and transient fields
                if (Modifier.isFinal(field.getModifiers()) ||
                        Modifier.isStatic(field.getModifiers()) ||
                        Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                boolean hasOneToMany = field.isAnnotationPresent(OneToMany.class);
                boolean hasManyToOne = field.isAnnotationPresent(ManyToOne.class);
                boolean hasOneToOne = field.isAnnotationPresent(OneToOne.class);
                boolean hasManyToMany = field.isAnnotationPresent(ManyToMany.class);

                if (!hasOneToMany && !hasManyToOne && !hasOneToOne && !hasManyToMany) {
                    continue;
                }

                field.setAccessible(true);

                if (hasOneToMany) {
                    try {
                        Object fieldValue = field.get(obj);
                        if (fieldValue instanceof Collection<?> collection) {
                            for (Object child : collection) {
                                if (child != null && isEntityClass(child.getClass()) &&
                                        hasBidirectionalReference(child.getClass(), objClass)) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "Error accessing OneToMany field " + field.getName() + ": " + e.getMessage());
                    }
                }

                // Check ManyToOne relationship
                if (hasManyToOne) {
                    try {
                        Class<?> fieldType = field.getType();
                        if (isEntityClass(fieldType) &&
                                hasBidirectionalReference(fieldType, objClass)) {
                            return true;
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking ManyToOne field " + field.getName() + ": " + e.getMessage());
                    }
                }

                // Check OneToOne relationship
                if (hasOneToOne) {
                    try {
                        Class<?> fieldType = field.getType();
                        if (isEntityClass(fieldType) &&
                                hasBidirectionalReference(fieldType, objClass)) {
                            return true;
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking OneToOne field " + field.getName() + ": " + e.getMessage());
                    }
                }

                // Check ManyToMany relationship
                if (hasManyToMany) {
                    try {
                        Object fieldValue = field.get(obj);
                        if (fieldValue instanceof Collection<?> collection) {
                            for (Object child : collection) {
                                if (child != null && isEntityClass(child.getClass()) &&
                                        hasBidirectionalReference(child.getClass(), objClass)) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "Error accessing ManyToMany field " + field.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking bidirectional relationship: " + e.getMessage());
        }

        return false;
    }

    private boolean isEntityClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class);
    }

    private boolean hasBidirectionalReference(Class<?> targetClass, Class<?> sourceClass) {
        for (Field field : targetClass.getDeclaredFields()) {
            Class<?> fieldType = field.getType();

            if (fieldType.equals(sourceClass)) {
                return true;
            }

            if (Collection.class.isAssignableFrom(fieldType)) {
                try {
                    ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                    Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?>) {
                        Class<?> collectionType = (Class<?>) typeArgs[0];
                        if (collectionType.equals(sourceClass)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return false;
    }
}
