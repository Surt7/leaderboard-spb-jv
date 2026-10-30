package com.worldpinball.leaderboard.infrastructure.config;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlConfig {

    @Bean
    public MaxQueryDepthInstrumentation maxQueryDepthInstrumentation() {
        return new MaxQueryDepthInstrumentation(8);
    }

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType longScalar = GraphQLScalarType.newScalar()
                .name("Long")
                .description("64-bit integral scalar")
                .coercing(new Coercing<Long, Long>() {
                    @Override
                    public Long serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof Number n) {
                            return n.longValue();
                        }
                        if (dataFetcherResult instanceof String s) {
                            try {
                                return Long.parseLong(s);
                            } catch (NumberFormatException e) {
                                throw new CoercingSerializeException("Unable to serialize Long: " + s, e);
                            }
                        }
                        throw new CoercingSerializeException("Expected a Number for Long scalar");
                    }

                    @Override
                    public Long parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof Number n) {
                            return n.longValue();
                        }
                        if (input instanceof String s) {
                            try {
                                return Long.parseLong(s);
                            } catch (NumberFormatException e) {
                                throw new CoercingParseValueException("Unable to parse Long: " + s, e);
                            }
                        }
                        throw new CoercingParseValueException("Expected a Number for Long scalar");
                    }

                    @Override
                    public Long parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof IntValue iv) {
                            return iv.getValue().longValue();
                        }
                        if (input instanceof StringValue sv) {
                            try {
                                return Long.parseLong(sv.getValue());
                            } catch (NumberFormatException e) {
                                throw new CoercingParseLiteralException("Unable to parse Long literal", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected IntValue for Long scalar");
                    }
                })
                .build();

        return wiringBuilder -> wiringBuilder.scalar(longScalar);
    }
}
