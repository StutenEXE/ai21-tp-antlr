package fr.utc.model;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import fr.utc.gui.Traceur;
import fr.utc.parsing.LogoParser.AvContext;
import fr.utc.parsing.LogoParser.FloatContext;
import fr.utc.parsing.LogoParser;
import fr.utc.parsing.LogoParser.*;
import javafx.beans.property.StringProperty;

public class LogoTreeVisitor extends LogoStoppableTreeVisitor {
	fr.utc.gui.Traceur traceur;
	Log log;
	private Deque<Integer> loopMemory; // Memoir LIFO de mes loops
	private Deque<Map<String, Double>> tableSymbole;

	public LogoTreeVisitor() {
		traceur = new Traceur();
		log = new Log();
		this.loopMemory = new ArrayDeque<Integer>();  // Memoire des loop LIFO
		this.tableSymbole = new ArrayDeque<Map<String,Double>>(); // table des symboles LIFO
		this.tableSymbole.push(new HashMap<String, Double>()); // table des symboles du bloc principal
	}

	public StringProperty logProperty() {
		return log;
	}

	public fr.utc.gui.Traceur getTraceur() {
		return traceur;
	}

	/*
	 * Map des attributs associés à chaque noeud de l'arbre
	 * key = node, value = valeur de l'expression du node
	 */
	ParseTreeProperty<Double> atts = new ParseTreeProperty<Double>();

	public void setValue(ParseTree node, double value) {
		atts.put(node, value);
	}

	public double getValue(ParseTree node) {
		Double value = atts.get(node);
		if (value == null) {
			throw new NullPointerException();
		}
		return value;
	}

	@Override
	public Integer visitBc(BcContext ctx){
		traceur.setCrayon(true);
		log.defaultLog(ctx);
		log.appendLog("Crayon baisser");
		return 0;
	}
	
	@Override
	public Integer visitFpos(FposContext ctx){
		Pair<Integer, List<Double>> bilan = evaluateList(ctx.expr());
		if(bilan.a == 0 && bilan.b.size()==2) {
			traceur.teleport(bilan.b.get(0), bilan.b.get(1));
			log.defaultLog(ctx);
			log.appendLog("Possition définit à : ", String.valueOf(bilan.b));
			
			return 0;
		}
		return 1;
	}
	
	@Override
	public Integer visitLc(LcContext ctx){
		traceur.setCrayon(false);
		log.defaultLog(ctx);
		log.appendLog("Crayon Lever");
		return 0;
	}

	@Override
	public Integer visitTd(TdContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a droite");
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitRe(ReContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Recule de ", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitTg(TgContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a gauche");
		}
		return bilan.a;
	}
	
	@Override 
	public Integer visitFcc(FccContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.setColor(bilan.b.intValue());
			log.defaultLog(ctx);
			log.appendLog("Couleur changer pour la n°", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitFcap(FcapContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			log.defaultLog(ctx);
			log.appendLog("Cap de la tortue fixé à : ", String.valueOf(bilan.b));
			traceur.fixeCap(bilan.b);
		}
		return bilan.a;
	}

	@Override
	public Integer visitAv(AvContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(bilan.b);
			// Différents type de log possibles. Voir classe Log
			log.defaultLog(ctx);
			log.appendLog("Avance de", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitStore(StoreContext ctx) {
		traceur.store();
		log.defaultLog(ctx);
		log.appendLog("Position sauvegardé");
		return 0;
	}
	
	@Override
	public Integer visitMove(MoveContext ctx) {
		log.defaultLog(ctx);
		if(!traceur.move()) {
			log.appendLog("Impossible de changer de position, aucune position sauvegardé");
		}else {
			log.appendLog("Changement pour l'ancienne position sauvegardé");	
		}
		return 0;
	}
	
	@Override
	public Integer visitAffectation(AffectationContext ctx) {
		Pair<Integer, Double> b = evaluate(ctx.expr());
		if (b.a==0) {
			log.appendLog("Symbol : ", ctx.VAR().getText(), " Définit à : ", String.valueOf(b.b));
			this.tableSymbole.peek().put(ctx.VAR().getText(), b.b);
		}
		return b.a;
	}
	
	// Expressions

	@Override
	public Integer visitFloat(FloatContext ctx) {
		String floatText = ctx.FLOAT().getText();
		setValue(ctx, Double.valueOf(floatText));
		return 0;
	}
	
	@Override
	public Integer visitParenthese(ParentheseContext ctx) {
		// Visite récursive de l'expression interne
	    int b = visit(ctx.expr());
	    if (b == 0) {
	        Double val = getValue(ctx.expr());
	        setValue(ctx, val);
	    }
	    return b;
	}
	
	@Override
	public Integer visitSum(SumContext ctx) {
		Pair<Integer, Double> left, right;
		left = evaluate(ctx.expr(0));
		right = evaluate(ctx.expr(1));
		if (left.a == 0 && right.a == 0) {
			String sign = ctx.getChild(1).getText();
			Double r = sign.equals("+") ? left.b + right.b :
			left.b - right.b;
			setValue(ctx, r);
			return 0;
		}
		return left.a == 0 ? right.a : left.a;
	}
	
	@Override
	public Integer visitMult(MultContext ctx) {
		Pair<Integer, Double> left, right;
		left = evaluate(ctx.expr(0));
		right = evaluate(ctx.expr(1));
		if (left.a == 0 && right.a == 0) {
			String sign = ctx.getChild(1).getText();
			Double r = Double.POSITIVE_INFINITY;
			if(sign.equals("*")) {
				r = left.b * right.b;
			}else {
				if(right.b != 0) {
					r = left.b / right.b;
				}else {
					return 1;
				}
			}
			setValue(ctx, r);
			return 0;
		}
		return left.a == 0 ? right.a : left.a;
	}
	
	@Override
	public Integer visitHasard(HasardContext ctx) {
		int b = visit(ctx.expr());
	    if (b == 0) {
	        Double max = getValue(ctx.expr());
	        double result = Math.random() * max;
	        setValue(ctx, result);
	    }
	    return b;
	}
	
	@Override
	public Integer visitCos(CosContext ctx) {
		int b = visit(ctx.expr());
	    if (b == 0) {
	        Double value = getValue(ctx.expr());
	        double result = Math.cos(value);
	        setValue(ctx, result);
	    }
	    return b;
	}
	
	@Override
	public Integer visitSin(SinContext ctx) {
		int b = visit(ctx.expr());
	    if (b == 0) {
	        Double value = getValue(ctx.expr());
	        double result = Math.sin(value);
	        setValue(ctx, result);
	    }
	    return b;
	}
	
	@Override
	public Integer visitRepete(RepeteContext ctx) {
		int b = visit(ctx.expr());
		if(b==0) {
			// Table des symboles du block
			this.tableSymbole.push(new HashMap<String, Double>());
			for (int i=0; i<getValue(ctx.expr()); i++) {
				// Ajout dans la pile de la valeur de la boucles
				this.loopMemory.push(i+1);
				for ( InstructionContext instuction : ctx.liste_instructions().instruction()){
					visit(instuction);
				}
				// Suppression de la valeur obscolète de la boucle
				this.loopMemory.pop();
			}	
			// On dépile les symbole
			this.tableSymbole.pop();
		}
		return b;
	}
	
	@Override
	public Integer visitLoop(LoopContext ctx) {
		if(this.loopMemory.isEmpty()) {
			log.appendLog("Impossible d'utiliser loop sans repétition");
			return 1;
		}
		Integer value = this.loopMemory.element();
		setValue(ctx, value);
		return 0;
	}
	
	@Override
	public Integer visitVariables(VariablesContext ctx) {
		for (Map<String, Double> mapDesBlock : this.tableSymbole) {
			if (mapDesBlock.containsKey(ctx.VAR().getText())){
				setValue(ctx, mapDesBlock.get(ctx.VAR().getText()));
				return 0;
			}
		}
		log.appendLog("Symbol \"", ctx.VAR().getText(), "\" est introuvable");
		return 1;
	}
	
	@Override
	public Integer visitComparaison(ComparaisonContext ctx) {
		Pair<Integer, Double> left, right;
		left = evaluate(ctx.expr(0));
		right = evaluate(ctx.expr(1));
		if (left.a == 0 && right.a == 0) {
			String sign = ctx.getChild(1).getText();
			log.defaultLog(ctx);
			if(sign.equals("<")) {
				setValue(ctx, left.b < right.b ? 1: 0);
				log.appendLog("Comparaison de  ", String.valueOf(left.b), " < ", String.valueOf(right.b));
			}else {
				setValue(ctx, left.b > right.b ? 1: 0);
				log.appendLog("Comparaison de  ", String.valueOf(left.b), " > ", String.valueOf(right.b));
			}
		}
		return left.a == 0 ? right.a : left.a;
	}

	/**
	 * Visite le noeud expression
	 * S'il n'y a pas d'erreur (la valeur de retour de la visite vaut 0)
	 * on récupère la valeur de l'expressions à partir de la map
	 * sinon
	 * on affecte une valeur quelconque
	 * On retourne une paire, (code de visite, valeur)
	 * 
	 * @param expr
	 * @return
	 */
	private Pair<Integer, Double> evaluate(ParseTree expr) {
		int b = visit(expr);
		Double val = b == 0 ? getValue(expr) : Double.POSITIVE_INFINITY;
		return new Pair<Integer, Double>(b, val);
	}
	
	private Pair<Integer, List<Double>> evaluateList(List<ExprContext> expr) {
		List<Double> values = new ArrayList<>();
		int code = 0;
		for(ExprContext e : expr) {
			int b = visit(e);
			Double val = b == 0 ? getValue(e) : Double.POSITIVE_INFINITY;
			code +=b;
			values.add(val);	
		}
		return new Pair<>(code, values);
	}

}
